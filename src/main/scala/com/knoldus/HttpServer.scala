package com.knoldus

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.Config

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import org.fusesource.jansi.Ansi.Color._
import org.fusesource.jansi.Ansi._

// $COVERAGE-OFF$
class HttpServer(httpConfig: Config)(implicit
  system: ActorSystem,
  executionContext: ExecutionContext,
  materializer: Materializer,
  logger: LoggingAdapter
) {

  def start(routes: Route): Future[Http.ServerBinding] = {
    val interface = httpConfig.getString("interface")
    val port = httpConfig.getInt("port")
    val prefix = httpConfig.getString("prefix")

    val prefixedRoutes = pathPrefix(prefix).apply(routes)

    val serverBinding = Http().newServerAt(interface, port).bind(prefixedRoutes)

    serverBinding.onComplete {
      case Success(_) =>
        logger.info("Has bound server on {}:{}.", interface, port)
        println(
          ansi()
            .fg(GREEN)
            .a("""
   ▄█   ▄█▄ ███▄▄▄▄    ▄██████▄   ▄█       ▀████    ▐████▀         ▄████████    ▄████████    ▄████████  ▄█    █▄   ▄█   ▄████████    ▄████████
  ███ ▄███▀ ███▀▀▀██▄ ███    ███ ███         ███▌   ████▀         ███    ███   ███    ███   ███    ███ ███    ███ ███  ███    ███   ███    ███
  ███▐██▀   ███   ███ ███    ███ ███          ███  ▐███           ███    █▀    ███    █▀    ███    ███ ███    ███ ███▌ ███    █▀    ███    █▀
 ▄█████▀    ███   ███ ███    ███ ███          ▀███▄███▀           ███         ▄███▄▄▄      ▄███▄▄▄▄██▀ ███    ███ ███▌ ███         ▄███▄▄▄
▀▀█████▄    ███   ███ ███    ███ ███          ████▀██▄          ▀███████████ ▀▀███▀▀▀     ▀▀███▀▀▀▀▀   ███    ███ ███▌ ███        ▀▀███▀▀▀
  ███▐██▄   ███   ███ ███    ███ ███         ▐███  ▀███                  ███   ███    █▄  ▀███████████ ███    ███ ███  ███    █▄    ███    █▄
  ███ ▀███▄ ███   ███ ███    ███ ███▌    ▄  ▄███     ███▄          ▄█    ███   ███    ███   ███    ███ ███    ███ ███  ███    ███   ███    ███
  ███   ▀█▀  ▀█   █▀   ▀██████▀  █████▄▄██ ████       ███▄       ▄████████▀    ██████████   ███    ███  ▀██████▀  █▀   ████████▀    ██████████
  ▀                              ▀                                                          ███    ███
   """).reset()
        )
      case Failure(ex) =>
        logger.error(ex, "Has failed to bind to {}:{}!", interface, port)
    }

    serverBinding
  }

}

// $COVERAGE-ON$
