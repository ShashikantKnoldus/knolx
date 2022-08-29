package com.knoldus.routes.feedbackform

import java.util.Date
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.feedbackform.{ FeedbackForm, UserFeedbackResponse }
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.feedbackform.FeedbackSession
import com.knoldus.routes.contract.user.UserResponse
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.feedbackform.FeedbackFormResponseService
import com.knoldus.services.feedbackform.FeedbackFormResponseService.FeedbackFormResponseServiceError
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.Future

class FeedbackFormResponseRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {

    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: FeedbackFormResponseService = mock[FeedbackFormResponseService]
    val feedbackFormResponse = new FeedbackFormResponseRoutes(service, authorizationService)
    val routes: Route = new FeedbackFormResponseRoutes(service, authorizationService).routes
    val error: FeedbackFormResponseServiceError = mock[FeedbackFormResponseServiceError]
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
        lastBannedOn = None,
        nonParticipating = false,
        department = None
      )
    val result: Future[UserToken] = cache.getOrLoad("token", _ => Future(UserToken("token", date, userInformation)))
    val withIdUser: WithId[UserInformation] = WithId(entity = userInformation, "1")

    val storedUserResponse: UserFeedbackResponse = UserFeedbackResponse(
      randomId,
      randomId,
      coreMember = randomBoolean,
      "randomMailId@knoldus.com",
      List(),
      meetup = randomBoolean,
      "random@knoldus.in",
      new Date(1575417600),
      0.0,
      "session 1",
      "tab",
      new Date()
    )

    val feedbackSession: FeedbackSession = FeedbackSession(
      randomId,
      "praful.bangar@knoldus.com",
      new Date(),
      "session 1",
      randomId,
      "tab",
      meetup = randomBoolean,
      "Good",
      cancelled = randomBoolean,
      active = randomBoolean,
      randomId,
      new Date()
    )

    val feedbackFormEntity: FeedbackForm = FeedbackForm("formName", List(), active = randomBoolean)

    val userResponse: UserResponse = UserResponse(
      "praful.bangar@knoldus.com",
      active = randomBoolean,
      admin = randomBoolean,
      coreMember = randomBoolean,
      superUser = randomBoolean,
      new Date(),
      randomInteger,
      None
    )
  }

  "Get /fetch Stored Responses for particular session" should {
    "return stored Responses from feedbackformsresponse" in new Setup {
      service.fetchFeedbackResponse(any[String], any[String]) shouldReturn future(
            Right(WithId(storedUserResponse, randomId))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedbacks/fetch-feedback-response?userId=$randomId&sessionId=$randomId")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get /error message for fetchFeedbackResponse" should {
    "return error while fetching response" in new Setup {
      service.fetchFeedbackResponse(any[String], any[String]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedbacks/fetch-feedback-response?userId=$randomId&sessionId=$randomId")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get /FeedbackForms for today" should {
    "return all active feedbackForms for today" in new Setup {
      service.getFeedbackFormForToday(any[String]) shouldReturn future(
            Right(List((feedbackSession, WithId(feedbackFormEntity, randomId))))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedbacks/getFeedbackFormForToday?userId=$randomId")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get /return error for getFeedbackFormForToday " should {
    "return error while getting active feedbackForms" in new Setup {
      service.getFeedbackFormForToday(any[String]) shouldReturn future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedbacks/getFeedbackFormForToday?userId=$randomId")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Post /store user response on the session" should {
    "store user responses for todays feedbacks" in new Setup {
      service.storeFeedbackResponse(
        any[String],
        any[String],
        any[String],
        any[List[String]],
        any[Double]
      ) shouldReturn future(
            Right(storedUserResponse)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse(""" {
                                       |   "sessionId":"5de240bc7600009400dffdfa",
                                       |   "feedbackFormId":"5de23f6622d5f1718611ed86",
                                       |   "responses":[
                                       |        "2"
                                       |    ],
                                       |   "score":50.9
                                       | } """".stripMargin)

      Post(s"/feedbacks/store-feedback?userId=$randomId", body)
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Post /return error for storeFeedbacksResponse" should {
    "return error while storing responses" in new Setup {
      service.storeFeedbackResponse(
        any[String],
        any[String],
        any[String],
        any[List[String]],
        any[Double]
      ) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse(s""" {
                                        |   "sessionId":"5de240bc7600009400dffdfa",
                                        |   "feedbackFormId":"5de23f6622d5f1718611ed86",
                                        |   "responses":[
                                        |        "2"
                                        |    ],
                                        |   "score":50.9
                                        | } """".stripMargin)

      Post(s"/feedbacks/store-feedback?userId=$randomId", body)
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get /Ban users details" should {
    "return ban users information" in new Setup {
      service.getBanUserDetails(any[String]) shouldReturn future(
            Right(userResponse)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedbacks/bannedusers?userId=$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /return error for getBanUserDetails" should {
    "return error for ban users information" in new Setup {
      service.getBanUserDetails(any[String]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedbacks/bannedusers?userId=$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "check Translate error for FeedbackAccessDeniedError " should {
    "return translate error" in new Setup {
      feedbackFormResponse.translateError(FeedbackFormResponseServiceError.FeedbackAccessDeniedError) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, " Access Denied Error", None, List()))

    }
  }

  "check Translate error for BadRequest " should {
    "return translate error" in new Setup {
      feedbackFormResponse.translateError(FeedbackFormResponseServiceError.FeedbackError) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "Feedback Response Error", None, List()))

    }
  }

  "check Translate error for NotFound error " should {
    "return translate error" in new Setup {
      feedbackFormResponse.translateError(FeedbackFormResponseServiceError.FeedbackNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Feedback Response Not Found", None, List()))

    }
  }

}
