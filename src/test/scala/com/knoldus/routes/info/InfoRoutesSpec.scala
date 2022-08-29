package com.knoldus.routes.info

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.knoldus.domain.info.Status
import com.knoldus.routes.RoutesSpec
import com.knoldus.services.info.InfoService
import play.api.libs.json.{ JsObject, JsValue, Json }
import com.knoldus.utils.json.CommonFormats.DurationFormat

import scala.concurrent.duration.Duration

class InfoRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val service: InfoService = mock[InfoService]
    val routes: Route = new InfoRoutes(service).routes
  }

  "GET /status endpoint" should {

    "return current status" in new Setup {
      service.getUptime shouldReturn future(Status(Duration.fromNanos(100000L), conf.getString("api-version")))
      Get("/status").check {
        status shouldBe StatusCodes.OK
        (responseAs[JsObject] \ "uptime").as[Duration].length should be > 0L
      }
    }
  }

  "GET /health endpoint" should {
    "return healthy flag" in new Setup {
      service.getDbStatus shouldReturn true
      Get("/health").check {
        status shouldBe StatusCodes.OK
        val res = Json.parse("""
                               |{
                               |"isHealthy":true
                               |}
                               |""".stripMargin)

        assert(responseAs[JsValue] == res)
      }
    }

    "return unhealthy flag" in new Setup {
      service.getDbStatus shouldReturn false
      Get("/health").check {
        status shouldBe StatusCodes.OK
        val res = Json.parse("""
                               |{
                               |"isHealthy":false
                               |}
                               |""".stripMargin)

        assert(responseAs[JsValue] == res)
      }
    }

  }

}
