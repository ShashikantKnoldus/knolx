package com.knoldus.routes.feedbackform

import java.util.Date
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.feedbackform.{ FeedbackReport, ReportResult }
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.feedbackform.FeedbackFormReportService
import com.knoldus.services.feedbackform.FeedbackFormReportService.FeedbackFormReportServiceError
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future

class FeedbackFormReportRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: FeedbackFormReportService = mock[FeedbackFormReportService]
    val feedbackFormReportRoute: FeedbackFormReportRoutes = new FeedbackFormReportRoutes(service, authorizationService)
    val routes: Route = new FeedbackFormReportRoutes(service, authorizationService).routes
    val error: FeedbackFormReportServiceError = mock[FeedbackFormReportServiceError]
    val randomId: String = RandomGenerators.randomString()
    val randomInteger: Int = RandomGenerators.randomInt(10)
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
        lastBannedOn = None,
        nonParticipating = false,
        department = None
      )

    val withIdUser: WithId[UserInformation] = WithId(entity = userInformation, "1")
    val result: Future[UserToken] = cache.getOrLoad("token", _ => Future(UserToken("token", date, userInformation)))
  }

  "Get/Reports for session by user" should {

    "manage user FeedbackReports" in new Setup {
      when(service.manageUserFeedbackReports(any[String], any[Int])) thenReturn future(
            Right(ReportResult(List(), randomInteger, randomInteger))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/knolx-reports/$randomId?pageNumber=$randomInteger")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get/Return error for Reports for session by user" should {

    "get error manage user FeedbackReports" in new Setup {
      when(service.manageUserFeedbackReports(any[String], any[Int])) thenReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/knolx-reports/$randomId?pageNumber=$randomInteger")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get/Fetch Reports by session Id" should {

    "manage user FeedbackReports" in new Setup {
      when(service.fetchUserResponsesBySessionId(any[String], any[String])) thenReturn future(
            Right(FeedbackReport(None, List()))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/knolx-reports/fetch-user-response-by-session?userId=$randomId&sessionId=$randomId")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }

    }
  }

  "Get/return error for Fetch Reports by session Id" should {

    "return error in manage user FeedbackReports" in new Setup {
      when(service.fetchUserResponsesBySessionId(any[String], any[String])) thenReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/knolx-reports/fetch-user-response-by-session?userId=$randomId&sessionId=$randomId")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get/Manage All user reports" should {

    "manage all  user FeedbackReports" in new Setup {
      when(service.manageAllFeedbackReports(any[Int])(any[NewUserInformation])) thenReturn future(
            Right(ReportResult(List(), randomInteger, randomInteger))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/reports/all-feedback-reports?pageNumber=$randomInteger")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get/return error for Manage All user reports" should {

    "checked error for manage all  user FeedbackReports" in new Setup {
      when(service.manageAllFeedbackReports(any[Int])(any[NewUserInformation])) thenReturn future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/reports/all-feedback-reports?pageNumber=$randomInteger")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get/Manage All user Feedbackreports" should {

    "manage all  user FeedbackReports" in new Setup {
      when(service.manageAllFeedbackReports(any[Int])(any[NewUserInformation])) thenReturn future(
            Right(ReportResult(List(), randomInteger, randomInteger))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/reports/all-feedback-reports?pageNumber=1").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get/return error in Manage All user Feedbackreports" should {

    "checked error in manage all  user FeedbackReports" in new Setup {
      when(service.manageAllFeedbackReports(any[Int])(any[NewUserInformation])) thenReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/reports/all-feedback-reports?pageNumber=$randomInteger")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get/fetch Report for All user reports" should {
    "fetch all  user FeedbackResponses" in new Setup {
      when(service.fetchAllResponsesBySessionId(any[String], any[String])(any[NewUserInformation])) thenReturn future(
            Right(FeedbackReport(None, List()))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/reports/$randomId?sessionId=$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get/return error for fetch Report for All user reports" should {
    "checked error in fetch all  user FeedbackResponses" in new Setup {
      when(service.fetchAllResponsesBySessionId(any[String], any[String])(any[NewUserInformation])) thenReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/reports/$randomInteger?sessionId=$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "check Translate error for FeedbackAccessDeniedError " should {
    "return translate error" in new Setup {
      feedbackFormReportRoute.translateError(FeedbackFormReportServiceError.FeedbackAccessDeniedError) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, " Access Denied Error", None, List()))

    }
  }

  "check Translate error for NotFound error " should {
    "return translate error" in new Setup {
      feedbackFormReportRoute.translateError(FeedbackFormReportServiceError.FeedbackNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Feedback Report Not Found", None, List()))

    }
  }

  "check Translate error for BadRequest " should {
    "return translate error" in new Setup {
      feedbackFormReportRoute.translateError(FeedbackFormReportServiceError.FeedbackError) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "Feedback Report Error", None, List()))

    }
  }

  "check Translate error for user not found " should {
    "return translate error" in new Setup {
      feedbackFormReportRoute.translateError(FeedbackFormReportServiceError.UserNotFound) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "User Not Found ", None, List()))

    }
  }

}
