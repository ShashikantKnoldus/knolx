package com.knoldus.routes.recommendation

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.recommendation.{ Recommendation, RecommendationsResponse }
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.recommendation.{
  AddRecommendationResponse,
  RecommendationInformation,
  RecommendationResponse
}
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.recommendation.RecommendationService
import com.knoldus.services.recommendation.RecommendationService.RecommendationServiceError
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{ JsValue, Json }

import java.util.Date
import scala.concurrent.Future

class RecommendationRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: RecommendationService = mock[RecommendationService]
    val recommendationRoutes: RecommendationRoutes = new RecommendationRoutes(service, authorizationService)
    val routes: Route = new RecommendationRoutes(service, authorizationService).routes
    val error: RecommendationServiceError = mock[RecommendationServiceError]
    val id: String = RandomGenerators.randomString()
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

    val recommendationEntity: Recommendation = Recommendation(
      Some("srg@gmail.com"),
      "sakshi",
      "topic1",
      "description1",
      new Date,
      new Date,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      0,
      0
    )

    val suggestedRecommendation: AddRecommendationResponse = AddRecommendationResponse(
      Some("srg@gmail.com"),
      "sakshi",
      "topic1",
      "description1",
      new Date,
      new Date,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      0,
      0,
      id
    )

    val response: RecommendationResponse = RecommendationResponse("srg@gmail.com", id, randomBoolean, randomBoolean)

    val recommendationsResponse: RecommendationsResponse =
      RecommendationsResponse(testEmail, "recommendationId", upVote = true, downVote = false)

    val recommendationInformation: RecommendationInformation =
      RecommendationInformation(Some("srg@gmail.com"), "sakshi", "topic1", "description1")

    val recommendation: Recommendation = Recommendation(
      Some("email"),
      "name",
      "topic",
      "description",
      new Date(),
      new Date(),
      approved = true,
      decline = false,
      pending = false,
      done = true,
      book = false,
      2,
      3
    )
  }

  "POST /add Recommendation" should {

    "suggest Recommendation by post directive" in new Setup {
      service.addRecommendation(recommendationInformation) shouldReturn future(
            Right(WithId(recommendationEntity, id))
          )
      val body: JsValue = Json.parse(""" {
                                       |	"email":"srg@gmail.com",
                                       |	"name":"sakshi",
                                       |	"topic":"topic1",
                                       |	"description":"description1"
                                       |} """".stripMargin)
      Post("/recommendation", body).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "POST /add Recommendation for Error" should {

    "suggest Recommendation by post directive for error" in new Setup {
      service.addRecommendation(recommendationInformation) shouldReturn future(
            Left(error)
          )
      val body: JsValue = Json.parse(""" {
                                       |	"email":"srg@gmail.com",
                                       |	"name":"sakshi",
                                       |	"topic":"topic1",
                                       |	"description":"description1"
                                       |} """".stripMargin)
      Post("/recommendation", body).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "GET /get recommendation " should {

    "return recommendation" in new Setup {
      service.getRecommendationById(any[String]) shouldReturn future(
            Right(WithId(recommendationEntity, id))
          )
      Get(s"/recommendation/$id").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "GET /get recommendation error" should {

    "return recommendation error" in new Setup {
      service.getRecommendationById(any[String]) shouldReturn future(
            Left(error)
          )
      Get(s"/recommendation/$id").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /approve recommendation endpoints" should {
    "return current status" in new Setup {

      service.approveRecommendation(any[String])(any[NewUserInformation]) shouldReturn Future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |}""".stripMargin)
      Post(s"/recommendation/approve/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "POST /approve recommendation for error" should {

    "return error" in new Setup {
      service.approveRecommendation(any[String])(any[NewUserInformation]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/approve/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /decline recommendation endpoints" should {
    "return current status" in new Setup {

      service.declineRecommendation(any[String])(any[NewUserInformation]) shouldReturn Future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |}""".stripMargin)
      Post(s"/recommendation/decline/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "POST /decline recommendation endpoints" should {

    "return error while declining recommendation" in new Setup {

      service.declineRecommendation(any[String])(any[NewUserInformation]) shouldReturn Future(Left(error))

      val body: JsValue = Json.parse("""
                                       |{
                                       |}""".stripMargin)
      Post(s"/recommendation/decline/$id", body).addHeader(RawHeader("Authorization", "token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /approving recommendation for error" should {

    "return error" in new Setup {
      service.approveRecommendation(any[String])(any[NewUserInformation]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/decline/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /pending recommendation endpoints" should {
    "return current status" in new Setup {

      service.pendingRecommendation(any[String])(any[NewUserInformation]) shouldReturn Future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |}""".stripMargin)
      Post(s"/recommendation/pending/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "POST /pending recommendation for error" should {

    "return error" in new Setup {
      service.pendingRecommendation(any[String])(any[NewUserInformation]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/pending/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /done recommendation endpoints" should {
    "return current status" in new Setup {

      service.doneRecommendation(any[String])(any[NewUserInformation]) shouldReturn Future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |}""".stripMargin)
      Post(s"/recommendation/done/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "POST /done recommendation for error" should {

    "return error" in new Setup {
      service.doneRecommendation(any[String])(any[NewUserInformation]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/done/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "GET /all-pending-recommendation status endpoint " should {
    "return current status" in new Setup {
      when(service.allPendingRecommendation) thenReturn future(Right(randomInt))
      Get(s"/recommendation/pending-recommendations").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get/error message for all-pending-recommendation " should {
    "return error for all-pending-recommendation " in new Setup {

      when(service.allPendingRecommendation) thenReturn future(Left(error))

      Get("/recommendation/pending-recommendations").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /book-recommendation endpoints" should {
    "return current status" in new Setup {

      service.bookRecommendation(any[String])(any[NewUserInformation]) shouldReturn Future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |}""".stripMargin)
      Post(s"/recommendation/book-recommendation/$id", body)
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "POST /book-ecommendation for error" should {

    "return error" in new Setup {
      service.bookRecommendation(any[String])(any[NewUserInformation]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/book-recommendation/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /cancel-booked-recommendation endpoints" should {
    "return current status" in new Setup {

      service.cancelBookedRecommendation(any[String])(any[NewUserInformation]) shouldReturn Future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |}""".stripMargin)
      Post(s"/recommendation/cancel-booked-recommendation/$id", body)
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "POST /cancel-booked-recommendation for error" should {

    "return error" in new Setup {
      service.cancelBookedRecommendation(any[String])(any[NewUserInformation]) shouldReturn future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/cancel-booked-recommendation/$id")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get/all recommendation" should {
    "list all recommendation" in new Setup {
      when(service.listRecommendation(any[String])) thenReturn future(
            Right(List(suggestedRecommendation))
          )
      Get(s"/recommendation/list-recommendations?filter=all").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get/all recommendation endpoint for error" should {
    "list all recommendation" in new Setup {
      when(service.listRecommendation(any[String])) thenReturn future(
            Left(error)
          )
      Get(s"/recommendation/list-recommendations?filter=all").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get/getVote rest endpoint status  " should {
    "return current status " in new Setup {

      when(service.getVote(any[String], any[String])) thenReturn future(Right(""))

      Get(s"/recommendation/get-vote?id=$id&email=srg@gmail.com").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get/getVote rest endpoint status  " should {
    "return error status " in new Setup {

      when(service.getVote(any[String], any[String])) thenReturn future(Left(error))

      Get(s"/recommendation/get-vote?id=$id&email=srg@gmail.com").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Put /upVote rest endpoint status  " should {
    "return current status " in new Setup {

      when(service.upVote(any[String], any[Boolean])(any[NewUserInformation])) thenReturn future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Put(s"/recommendation/up-vote?recommendationId=$id&alreadyVoted=true")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Put /upVote rest endpoint status  " should {
    "return error status " in new Setup {

      when(service.upVote(any[String], any[Boolean])(any[NewUserInformation])) thenReturn future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Put(s"/recommendation/up-vote?recommendationId=$id&alreadyVoted=true")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get/downVote rest endpoint status  " should {
    "return current status " in new Setup {

      when(service.downVote(any[String], any[Boolean])(any[NewUserInformation])) thenReturn future(
            Right(WithId(recommendationEntity, id))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Put(s"/recommendation/down-vote?id=$id&alreadyVoted=true")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get/downVote rest endpoint status  " should {
    "return error status " in new Setup {

      when(service.downVote(any[String], any[Boolean])(any[NewUserInformation])) thenReturn future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Put(s"/recommendation/down-vote?id=$id&alreadyVoted=true")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Post/updatevote rest endpoint status  " should {
    "return error status " in new Setup {
      val body: JsValue = Json.parse("""
                                       |{
                                       |"email":"srg@gmail.com",
                                       |"recommendationId":"1569492774160",
                                       |"upVote":true,
                                       |"downVote":false
                                       |}
                            """.stripMargin)
      service.insertResponse(any[RecommendationResponse]) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/update-vote", body).addHeader(RawHeader("Authorization", "token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Post/updatevote rest endpoint status  " should {
    "return result" in new Setup {

      val body: JsValue = Json.parse("""
                                       |{
                                       |"email":"srg@gmail.com",
                                       |"recommendationId":"1569492774160",
                                       |"upVote":true,
                                       |"downVote":false
                                       |}
                            """.stripMargin)
      service.insertResponse(any[RecommendationResponse]) shouldReturn Future(
            Right(WithId(recommendationsResponse, "id"))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/recommendation/update-vote", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "check Translate error for RecommendationError " should {
    "return translate error" in new Setup {
      recommendationRoutes.translateError(RecommendationService.RecommendationServiceError.RecommendationError) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "Recommendation Error", None, List()))
    }
  }

  "check Translate error for InvalidRecommendation " should {
    "return translate error" in new Setup {
      recommendationRoutes.translateError(
        RecommendationService.RecommendationServiceError.InvalidRecommendationError
      ) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "Invalid Recommendation", None, List()))

    }
  }

  "check Translate error for RecommendationAccessDeniedError " should {
    "return translate error" in new Setup {
      recommendationRoutes.translateError(
        RecommendationService.RecommendationServiceError.RecommendationAccessDeniedError
      ) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Access Denied", None, List()))

    }
  }

  "check Translate error for RecommendationNotFoundError " should {
    "return translate error" in new Setup {
      recommendationRoutes.translateError(
        RecommendationService.RecommendationServiceError.RecommendationNotFoundError
      ) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Recommendation Not Found", None, List()))

    }
  }

}
