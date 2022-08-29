package com.knoldus.routes.feedbackform

import java.util.Date
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.feedbackform._
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.feedbackform.FeedbackFormData
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.feedbackform.FeedbackFormService
import com.knoldus.services.feedbackform.FeedbackFormService.FeedbackFormServiceError
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.Future

class FeedbackFormRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val service: FeedbackFormService = mock[FeedbackFormService]
    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val feedbackFormRoutes: FeedbackFormRoutes = new FeedbackFormRoutes(service, authorizationService)
    val routes: Route = new FeedbackFormRoutes(service, authorizationService).routes

    val error: FeedbackFormServiceError = mock[FeedbackFormServiceError]
    val randomId: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
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

    val feedbackFormInfo: FeedbackForm =
      FeedbackForm(
        "formName",
        List(Question("question", List("1"), "MCQ", mandatory = randomBoolean)),
        active = randomBoolean
      )

    val feedbackFormList: FeedbackFormList =
      FeedbackFormList(
        "Form",
        List(Question("question", List("1"), "MCQ", mandatory = randomBoolean)),
        active = randomBoolean,
        "id"
      )

    val question = List(
      Question("how much you like the session?", List("1", "2", "3"), "MCQ", mandatory = randomBoolean)
    )
  }

  "POST /Create Feedbackform" should {

    "create Feedbackform by post directive" in new Setup {
      service.createFeedbackForm("formName", question) shouldReturn future(
            Right(WithId(feedbackFormInfo, s"$randomId"))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse(s""" {
                                        | 	"name":"formName",
                                        | 	"questions": [{
                                        | 		"question": "how much you like the session?",
                                        | 		"options": ["1", "2", "3"],
                                        | 		"questionType": "MCQ",
                                        | 		"mandatory":$randomBoolean
                                        | 	}]
                                        |
                                        | } """".stripMargin)
      Post(s"/feedback-form/create-feedback-form", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {

        status shouldBe StatusCodes.OK
      }
    }
  }

  "POST /Create Feedbackform for error" should {

    "create Feedbackform by post directive check error" in new Setup {
      service.createFeedbackForm("formName", question) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse(s""" {
                                        | 	"name":"formName",
                                        | 	"questions": [{
                                        | 		"question": "how much you like the session?",
                                        | 		"options": ["1", "2", "3"],
                                        | 		"questionType": "MCQ",
                                        | 		"mandatory":$randomBoolean
                                        | 	}]
                                        |
                                        | } """".stripMargin)
      Post(s"/feedback-form/create-feedback-form", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "DELETE /check error" should {

    "return error" in new Setup {
      service.delete(s"$randomId") shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Delete(s"/feedback-form/$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "DELETE /status endpoint " should {

    "return current status" in new Setup {
      service.delete(s"$randomId") shouldReturn future(
            Right(())
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Delete(s"/feedback-form/$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Put/Update Feedback endpoint" should {

    "update data from feedbackform " in new Setup {
      when(
        service.update(any[String], any[Option[String]], any[Option[List[Question]]])(any[NewUserInformation])
      ) thenReturn future(
            Right(())
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |  "name" :"Praful"
                                       |}
                            """.stripMargin)

      Put(s"/feedback-form/$randomId", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Put/Update Feedback endpoint check error" should {

    "update data from feedbackform " in new Setup {
      when(
        service.update(any[String], any[Option[String]], any[Option[List[Question]]])(any[NewUserInformation])
      ) thenReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |  "name" :"Praful"
                                       |}
                            """.stripMargin)

      Put(s"/feedback-form/$randomId", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }
  "Get/Update Feedback endpoint" should {

    "get feedbackform  to update" in new Setup {
      when(service.getFeedbackFormById(any[String])(any[NewUserInformation])) thenReturn future(
            Right(FeedbackFormData("name", List(), active = randomBoolean, "as"))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedback-form/$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get/Update Feedback endpoint to check error" should {

    "check error" in new Setup {
      when(service.getFeedbackFormById(any[String])(any[NewUserInformation])) thenReturn future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedback-form/$randomId").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get/get list of  Feedbackforms " should {
    "return created feedbackform as list " in new Setup {

      when(service.listAllFeedbackForms(any[Int])(any[NewUserInformation])) thenReturn future(
            Right(Tuple2(Seq(WithId(feedbackFormInfo, s"$randomId")), randomInt))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedback-form/list-all?pageNumber=$randomInt")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get/get list of  Feedbackforms for sessions " should {
    "return created feedbackformas list used for sessions " in new Setup {

      when(service.getAllFeedbackForm(any[NewUserInformation])) thenReturn future(
            Right(List(feedbackFormList))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get("/feedback-form/all-forms").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get/get list of  Feedbackforms for sessions " should {
    "return error " in new Setup {

      when(service.getAllFeedbackForm(any[NewUserInformation])) thenReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get("/feedback-form/all-forms").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get/error message for list of feedbackforms " should {
    "return error for feedbackformas list " in new Setup {

      when(service.listAllFeedbackForms(any[Int])(any[NewUserInformation])) thenReturn future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/feedback-form/list-all?pageNumber=$randomInt")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "check Translate error for FeedbackAccessDeniedError " should {
    "return translate error" in new Setup {
      feedbackFormRoutes.translateError(FeedbackFormService.FeedbackServiceError.AccessDenied) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Access Denied", None, List()))

    }
  }

  "check Translate error for NotFound error " should {
    "return translate error" in new Setup {
      feedbackFormRoutes.translateError(FeedbackFormService.FeedbackServiceError.FeedbackNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Feedback Not Found", None, List()))

    }
  }

  "check Translate error for BadRequest " should {
    "return translate error" in new Setup {
      feedbackFormRoutes.translateError(FeedbackFormService.FeedbackServiceError.FeedbackError) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "Feedback Error", None, List()))

    }
  }

}
