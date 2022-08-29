package com.knoldus.routes.user

import java.util.Date
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.user.SessionsInformation
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.user.KnolxUserAnalysisService
import com.knoldus.services.user.KnolxUserAnalysisService.KnolxUserAnalysisServiceError
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future

class KnolxUserAnalysisRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {

    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: KnolxUserAnalysisService = mock[KnolxUserAnalysisService]
    val knolxUserAnalysisRoutes: KnolxUserAnalysisRoutes = new KnolxUserAnalysisRoutes(service, authorizationService)
    val routes: Route = new KnolxUserAnalysisRoutes(service, authorizationService).routes
    val sessionInfo: SessionsInformation = SessionsInformation("Topic", 66.00, 50.00)
    val dateTest: Date = new Date()
    val error: KnolxUserAnalysisServiceError = mock[KnolxUserAnalysisServiceError]
    val randomId: String = RandomGenerators.randomString()
    val randomInteger: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val testEmail = "test"
    val testName = "Testing"
    val dateTimeUtility = new DateTimeUtility
    val date = new Date(dateTimeUtility.toMillis(dateTimeUtility.localDateTimeIST.plusDays(1)))
    cache.remove("token")
    val validationDetails: KeycloakDetails = KeycloakDetails(testEmail, Admin)

    implicit val newUserInformation: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Admin
      )

    implicit val userInformation: UserInformation =
      UserInformation(
        testEmail,
        active = true,
        admin = true,
        coreMember = true,
        superUser = true,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )
    val result: Future[UserToken] = cache.getOrLoad("token", _ => Future(UserToken("token", date, userInformation)))
    val withIdUser: WithId[UserInformation] = WithId(entity = userInformation, "1")

    val userInfo: UserInformation = UserInformation(
      "email",
      active = randomBoolean,
      admin = randomBoolean,
      coreMember = randomBoolean,
      superUser = randomBoolean,
      dateTest,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val session: Session =
      Session(
        "userId",
        email = "random.test",
        dateTest,
        session = "Session1",
        category = "Database",
        subCategory = "Mysql",
        feedbackFormId = s"$randomId",
        topic = "CRUD",
        feedbackExpirationDays = randomInteger,
        meetup = randomBoolean,
        brief = "Major Topic",
        rating = "4",
        score = 0.0,
        cancelled = randomBoolean,
        active = randomBoolean,
        dateTest,
        youtubeURL = Some("www.knoldusYoutube.com"),
        slideShareURL = Some("www.KnoldusSlide.com"),
        temporaryYoutubeURL = Some("www.KnoldusTemp.com"),
        reminder = randomBoolean,
        notification = randomBoolean
      )

  }

  "Get/Comparison between User Sessions Response " should {

    "comparison between Responses with core member and non core member" in new Setup {
      when(service.userSessionsResponseComparison(any[String])(any[NewUserInformation])) thenReturn future(
            Right(List(sessionInfo))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/user-analytics/comparison?email=admin@knoldus.in")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error in comparing between Responses with core member and non core member" in new Setup {
      when(service.userSessionsResponseComparison(any[String])(any[NewUserInformation])) thenReturn future(
            Left(error)
          )
      Get(s"/user-analytics/comparison?email=admin@knoldus.in").addHeader(RawHeader("Authorization", "token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }

  "Get/Ban count of User " should {

    "return Ban count of users" in new Setup {
      when(service.getBanCount(any[String])(any[NewUserInformation])) thenReturn future(
            Right(WithId(userInfo, s"$randomId"))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/user-analytics/ban-count?email=admin@knoldus.in")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error for Ban count of users" in new Setup {
      when(service.getBanCount(any[String])(any[NewUserInformation])) thenReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/user-analytics/ban-count?email=admin@knoldus.in")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get/total meetups count of User " should {

    "return meetups count of users" in new Setup {
      when(service.getUserTotalMeetups(any[String])(any[NewUserInformation])) thenReturn future(
            Right(Map("totalMeetups" -> randomInteger))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/user-analytics/total-meetups?email=admin@knoldus.in")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error on meetups count of users" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      when(service.getUserTotalMeetups(any[String])(any[NewUserInformation])) thenReturn future(
            Left(KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError)
          )
      Get(s"/user-analytics/total-meetups?email=admin@knoldus.in")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.Unauthorized
        }
    }

  }

  "Get/total knolx count of User " should {

    "return knolx count of users" in new Setup {
      when(service.getUserTotalKnolx(any[String])(any[NewUserInformation])) thenReturn future(
            Right(Map("totalKnolx" -> randomInteger))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/user-analytics/total-knolx?email=admin@knoldus.in")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error on knolx count of users" in new Setup {
      when(service.getUserTotalKnolx(any[String])(any[NewUserInformation])) thenReturn future(
            Left(error)
          )
      Get(s"/user-analytics/total-knolx?email=admin@knoldus.in").addHeader(RawHeader("Authorization", "token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }

  "Get/total did not attend count of User " should {

    "return count of users not attended sessions" in new Setup {
      when(service.getUserDidNotAttendSessionCount(any[String])(any[NewUserInformation])) thenReturn future(
            Right(Map("didNotAttend" -> randomInteger))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/user-analytics/not-attended?email=admin@knoldus.in")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error on count of users not attended sessions" in new Setup {
      when(service.getUserDidNotAttendSessionCount(any[String])(any[NewUserInformation])) thenReturn future(
            Left(error)
          )
      Get(s"/user-analytics/not-attended?email=admin@knoldus.in").addHeader(RawHeader("Authorization", "token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

  }

  "check Translate error for User Analysis AccessDeniedError " should {
    "return translate error" in new Setup {
      knolxUserAnalysisRoutes.translateError(KnolxUserAnalysisServiceError.UserAnalysisError) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "User Analysis Error", None, List()))

    }
  }

  "check Translate error for NotFound error " should {
    "return translate error" in new Setup {
      knolxUserAnalysisRoutes.translateError(KnolxUserAnalysisServiceError.UserAnalysisNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "User Analysis Report Not Found", None, List()))

    }
  }

  "check Translate error for BadRequest " should {
    "return translate error" in new Setup {
      knolxUserAnalysisRoutes.translateError(KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, " Access Denied Error", None, List()))

    }
  }

}
