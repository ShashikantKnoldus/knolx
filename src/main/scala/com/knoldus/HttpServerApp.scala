// $COVERAGE-OFF$
package com.knoldus

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import akka.event.{ Logging, LoggingAdapter }
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings }
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.knoldus.bootstrap.{ DaoInstantiator, RoutesInstantiator, ServiceInstantiator }
import com.knoldus.domain.user.UserToken
import com.knoldus.dao.errormanagement.ErrorManagement
import com.knoldus.services.rollbarservice.RollbarService

import com.typesafe.config.ConfigFactory
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.{ MongoClient, MongoDatabase }

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success, Try }

@SuppressWarnings(Array("CatchThrowable"))
object HttpServerApp extends App {

  lazy val conf = ConfigFactory.load()
  val databaseConfig = conf.getConfig("database")
  val mongoClient = MongoClient(databaseConfig.getString("mongoURL"))
  val mongoDatabase = mongoClient.getDatabase(databaseConfig.getString("databaseName"))
  val daoInstantiator = new DaoInstantiator(mongoDatabase)
  implicit val actorSystem: ActorSystem = ActorSystem("knowledge-portal-actor-system")

  implicit lazy val errorManagement: ErrorManagement = new RollbarService(conf)
  errorManagement.init()

  implicit val materializer: Materializer =
    Materializer(actorSystem)
  implicit val logger: LoggingAdapter = Logging(actorSystem, "KnowledgePortalRest")

  implicit val executionContext: ExecutionContextExecutor = global

  val akkaShutdown = CoordinatedShutdown(actorSystem)
  val defaultCachingSettings = CachingSettings(actorSystem)

  val lfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(conf.getInt("cache.initialCapacity"))
      .withMaxCapacity(conf.getInt("cache.maxCapacity"))

  val cachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  val cache: Cache[String, UserToken] = LfuCache(cachingSettings)
  val clientSecretsCache: Cache[String, String] = LfuCache(cachingSettings)

  try {
    val httpServerConfig = conf.getConfig("http")

    val services = new ServiceInstantiator(conf, daoInstantiator, cache, clientSecretsCache, actorSystem)

    val httpServer = new HttpServer(httpServerConfig)(
      system = actorSystem,
      executionContext = actorSystem.dispatcher,
      materializer = materializer,
      logger = logger
    )

    val loadClientSecrets = new ClientCredential(daoInstantiator.clientDao)
    loadClientSecrets.storeCredentialsInCache(clientSecretsCache)
    val routes = new RoutesInstantiator(conf, services, cache)(actorSystem.dispatcher, logger).routes

    val serverBinding: Future[Http.ServerBinding] = httpServer.start(routes).andThen {
      case Failure(ex) => shutdown(ex)
    }

    akkaShutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "Unbinding http server") { () =>
      serverBinding.transformWith {
        case Success(binding) =>
          binding.unbind().andThen {
            case Success(_) => logger.info("Has unbounded http server.")
            case Failure(ex) => logger.error(ex, "Has failed to unbind http server.")
          }
        case Failure(_) => Future.successful(Done)
      }
    }
  } catch {
    case e: Throwable =>
      Await.result(shutdown(e), 30.seconds)
  }

  private def shutdown(e: Throwable): Future[Done] = {
    logger.error(e, "Error starting application:")
    akkaShutdown.run(new Reason {
      override def toString: String = "Error starting application: " ++ e.getMessage
    })
  }

  private def tryConnectToMongo(mongoDatabase: MongoDatabase): Document =
    Try(Await.result(mongoDatabase.runCommand(Document("ping" -> 1)).toFuture(), 30.seconds)) match {
      case Failure(exception) =>
        logger.error(s"Could not connect to Mongo on bootstrap. Error: $exception")
        throw exception
      case Success(value) =>
        value
    }

  tryConnectToMongo(mongoDatabase)

}
