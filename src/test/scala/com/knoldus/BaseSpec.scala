package com.knoldus

import akka.actor.Scheduler
import akka.dispatch.MessageDispatcher
import akka.event.LoggingAdapter
import akka.http.caching.LfuCache
import akka.http.caching.scaladsl.{ Cache, CachingSettings, LfuCacheSettings }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{ CallingThreadDispatcher, ImplicitSender, TestKit, TestKitBase }
import akka.util.Timeout
import com.knoldus.dao.errormanagement.ErrorManagement
import com.knoldus.domain.user.UserToken
import com.typesafe.config.{ Config, ConfigFactory }
import org.mockito.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{ Json, Writes }

import scala.concurrent.Future

trait BaseSpec
    extends AnyWordSpec
    with ScalatestRouteTest
    with TestKitBase
    with ImplicitSender
    with Matchers
    with ScalaFutures
    with IdiomaticMockito {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(Span(3, Seconds))
  implicit val errorManagement: ErrorManagement = new TestErrorManagement

  implicit val logger: LoggingAdapter = system.log
  implicit lazy val timeout: Timeout = Timeout(patienceConfig.timeout)
  implicit val ec: MessageDispatcher = system.dispatchers.lookup(CallingThreadDispatcher.Id)
  implicit val scheduler: Scheduler = system.scheduler

  protected val conf: Config = ConfigFactory.load()

  val defaultCachingSettings: CachingSettings = CachingSettings(system)

  val lfuCacheSettings: LfuCacheSettings =
    defaultCachingSettings.lfuCacheSettings
      .withInitialCapacity(conf.getInt("cache.initialCapacity"))
      .withMaxCapacity(conf.getInt("cache.maxCapacity"))

  val cachingSettings: CachingSettings =
    defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

  val cache: Cache[String, UserToken] = LfuCache(cachingSettings)
  val clientCache: Cache[String, String] = LfuCache(cachingSettings)

  def future[A](a: A): Future[A] = Future.successful(a)

  def future[A](throwable: Throwable): Future[A] = Future.failed[A](throwable)

  def httpEntity[A: Writes](entity: A): HttpEntity.Strict =
    HttpEntity(ContentTypes.`application/json`, Json.toJson(entity).toString)

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system, verifySystemShutdown = true)
  }

}
