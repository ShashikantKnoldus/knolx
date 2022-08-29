package com.knoldus.routes.statistic

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import com.knoldus.domain.statistic.{ KnolxDetails, Statistic }
import com.knoldus.routes.RoutesSpec
import com.knoldus.services.info.InfoService
import com.knoldus.services.statistic.StatisticalService
import com.knoldus.services.statistic.StatisticalService.StatisticalServiceError
import com.knoldus.services.usermanagement.AuthorizationService

import java.util.Date
import akka.http.scaladsl.server.Route
import com.knoldus.dao.user.UserDao
import com.knoldus.services.keyCloak.KeycloakIAMService
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

class StatisticalRouteSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val service: InfoService = mock[InfoService]
    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val statisticalService: StatisticalService = mock[StatisticalService]
    val routes: Route = new StatisticalRoutes(statisticalService, authorizationService).routes
    val dateTest: Date = new Date(1593491700000L)
    val error: StatisticalServiceError = mock[StatisticalServiceError]

    val statistic: Statistic =
      Statistic("randomString", "test", 2, List(KnolxDetails("randomString", "randomString", dateTest.toString)))
    clientCache.remove("clientSecret")
    val result: Future[String] = clientCache.getOrLoad("clientId", _ => Future("clientSecret"))
  }

  "GET/ GetStatistic endpoint" should {

    "return knolx details" in new Setup {
      statisticalService.getKnolxDetails(any[Option[Long]], any[Option[Long]]) shouldReturn Future(
            Right(Seq(statistic))
          )
      Get("/statistic").check {
        status shouldBe StatusCodes.OK
      }
    }
    "return error in getting knolx details" in new Setup {
      statisticalService.getKnolxDetails(any[Option[Long]], any[Option[Long]]) shouldReturn Future(
            Left(StatisticalServiceError.InvalidDates)
          )
      Get("/statistic").check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "return knolx details for specific time" in new Setup {
      val startDate = 1590969600000L
      val endDate = 1594512000000L
      statisticalService.getKnolxDetails(any[Option[Long]], any[Option[Long]]) shouldReturn Future(
            Right(Seq(statistic))
          )
      Get(s"/statistic?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error for specific time" in new Setup {
      val startDate = 1594512000000L
      val endDate = 1590969600000L
      statisticalService.getKnolxDetails(any[Option[Long]], any[Option[Long]]) shouldReturn Future(
            Left(StatisticalServiceError.InvalidDates)
          )
      Get(s"/statistic?startDate=$startDate&endDate=$endDate")
        .addHeader(RawHeader("clientId", "clientId"))
        .addHeader(RawHeader("clientSecret", "clientSecret"))
        .check {
          status shouldBe StatusCodes.BadRequest
        }
    }

    "return unauthorized when clientId is not provided" ignore new Setup {
      val startDate = 1594512000000L
      val endDate = 1590969600000L
      Get(s"/statistic?startDate=$startDate&endDate=$endDate")
        .addHeader(RawHeader("clientSecret", "clientSecret"))
        .check {
          status shouldBe StatusCodes.Unauthorized
        }
    }

    "return unauthorized when clientSecret is not provided" ignore new Setup {
      val startDate = 1594512000000L
      val endDate = 1590969600000L
      Get(s"/statistic?startDate=$startDate&endDate=$endDate").addHeader(RawHeader("clientId", "clientId")).check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return unauthorized when both client id and clientSecret is not provided" ignore new Setup {
      val startDate = 1594512000000L
      val endDate = 1590969600000L
      Get(s"/statistic?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.Unauthorized
      }
    }

    "return unauthorized when wrong client id and clientSecret provided" ignore new Setup {
      val startDate = 1594512000000L
      val endDate = 1590969600000L
      Get(s"/statistic?startDate=$startDate&endDate=$endDate")
        .addHeader(RawHeader("clientId", "wrongclientId"))
        .addHeader(RawHeader("clientSecret", "wrongclientSecret"))
        .check {
          status shouldBe StatusCodes.Unauthorized
        }
    }
    "return internal server error when clientId and clientSecret not found" in new Setup {
      val startDate = 1594512000000L
      val endDate = 1590969600000L
      statisticalService.getKnolxDetails(any[Option[Long]], any[Option[Long]]) shouldReturn Future(
            Left(error)
          )
      Get(s"/statistic?startDate=$startDate&endDate=$endDate").addHeader(RawHeader("clientId", "clientId")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }
}
