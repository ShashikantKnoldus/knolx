package com.knoldus.routes.session

import java.util.Date
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.session.{ Category, Session }
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.session.AddCategoryRequest
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.session.CategoryService
import com.knoldus.services.session.CategoryService.CategoryServiceError
import com.knoldus.services.usermanagement.AuthorizationService
import play.api.libs.json.{ JsValue, Json }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future

class CategoryRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {

    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: CategoryService = mock[CategoryService]
    val routes: Route = new CategoryRoutes(service, authorizationService).routes
    val categoryRoutes = new CategoryRoutes(service, authorizationService)
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val category: Category = Category("Primary", Seq("subCategory"))
    val withId: WithId[Category] = WithId(category, "1")
    val error: CategoryServiceError = mock[CategoryServiceError]
    val addCategoryRequest: AddCategoryRequest = AddCategoryRequest("Database", Seq())
    val sampleDate: Date = new Date(1569492774160L)
    val dateTest = new Date
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

    val sessionInfo: Session =
      Session(
        userId = randomString,
        email = randomString,
        dateTest,
        session = randomString,
        category = randomString,
        subCategory = randomString,
        feedbackFormId = randomString,
        topic = randomString,
        feedbackExpirationDays = randomInt,
        meetup = randomBoolean,
        brief = randomString,
        rating = randomString,
        score = 0.0,
        cancelled = randomBoolean,
        active = randomBoolean,
        dateTest,
        youtubeURL = Some(randomString),
        slideShareURL = Some(randomString),
        temporaryYoutubeURL = Some(randomString),
        reminder = randomBoolean,
        notification = randomBoolean
      )
    val withIdSession: WithId[Session] = WithId(entity = sessionInfo, randomString)
  }

  "Get /category endpoint" should {

    "return primary categories" in new Setup {

      service.getPrimaryCategories shouldReturn Future(Right(Seq(withId)))
      Get("/category").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /category endpoint" should {

    "return error" in new Setup {

      service.getPrimaryCategories shouldReturn Future(Left(error))
      Get("/category").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Post /category endpoint" should {

    "add given category" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.addPrimaryCategory(addCategoryRequest) shouldReturn Future(Right("1"))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"categoryName":"Database",
                                       |"subCategory":[]
                                       |}
                            """.stripMargin)
      Post("/category", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Post /category endpoint" should {

    "return error" in new Setup {

      service.addPrimaryCategory(addCategoryRequest) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"categoryName":"Database",
                                       |"subCategory":[]
                                       |}
                            """.stripMargin)
      Post("/category", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Put /category/id endpoint" should {

    "update given category" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.modifyPrimaryCategory("1", "Mongodb") shouldReturn Future(Right(()))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"newCategoryName":"Mongodb"
                                       |}
                            """.stripMargin)
      Put("/category/1", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Put /category/id endpoint" should {

    "return error" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.modifyPrimaryCategory("1", "Mongodb") shouldReturn Future(Left(error))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"newCategoryName":"Mongodb"
                                       |}
                            """.stripMargin)
      Put("/category/1", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Delete /category/id endpoint" should {

    "delete given primary categories" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.deletePrimaryCategory(any[String]) shouldReturn Future(Right(()))
      Delete("/category/1").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Delete /category/id endpoint" should {

    "return error" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.deletePrimaryCategory(any[String]) shouldReturn Future(Left(error))
      Delete("/category/1").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /subCategory endpoint" should {

    "return topics of subCategory" in new Setup {

      service.getTopicsBySubCategory(any[String], any[String]) shouldReturn Future(Right(Seq(withIdSession)))
      Get("/subcategory?categoryName=category&subCategory=subCategory").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /subCategory endpoint" should {

    "return error" in new Setup {

      service.getTopicsBySubCategory(any[String], any[String]) shouldReturn Future(Left(error))
      Get("/subcategory?categoryName=category&subCategory=subCategory").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /subCategory endpoint" should {

    "return subCategories of given category" in new Setup {

      service.getSubCategoryByPrimaryCategory(any[String]) shouldReturn Future(Right(withId))
      Get("/subcategory?categoryName=category").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /subCategory endpoint" should {

    "return error with no subCategory" in new Setup {

      service.getSubCategoryByPrimaryCategory(any[String]) shouldReturn Future(Left(error))
      Get("/subcategory?categoryName=category").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Post /subCategory endpoint" should {

    "add given subCategory" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.addSubCategory(any[String], any[String]) shouldReturn Future(Right(withId))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"categoryId":"1",
                                       |"subCategory":"Mysql"
                                       |}
                            """.stripMargin)
      Post("/subcategory", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Post /subCategory endpoint" should {

    "return error" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.addSubCategory(any[String], any[String]) shouldReturn Future(Left(error))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"categoryId":"1",
                                       |"subCategory":"Mysql"
                                       |}
                            """.stripMargin)
      Post("/subcategory", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Put /subCategory/id endpoint" should {

    "update given subCategory" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.modifySubCategory(any[String], any[String], any[String]) shouldReturn Future(Right(()))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"oldSubCategory":"Mongodb",
                                       |"newSubCategory":"Mysql"
                                       |}
                            """.stripMargin)
      Put("/subcategory/1", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Put /subCategory/id endpoint" should {

    "error" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.modifySubCategory(any[String], any[String], any[String]) shouldReturn Future(Left(error))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"oldSubCategory":"Mongodb",
                                       |"newSubCategory":"Mysql"
                                       |}
                            """.stripMargin)
      Put("/subcategory/1", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Delete /subCategory/id endpoint" should {

    "delete given subCategory" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.deleteSubCategory(any[String], any[String]) shouldReturn Future(Right(()))
      Delete("/subcategory/1?subCategory=subCategory").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Delete /subCategory/id endpoint" should {

    "return error" in new Setup {

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.deleteSubCategory(any[String], any[String]) shouldReturn Future(Left(error))
      Delete("/subcategory/1?subCategory=subCategory").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Check Translate error for Primary category not found " should {
    "return translate error" in new Setup {
      categoryRoutes.translateError(CategoryServiceError.PrimaryCategoryNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Primary Category Not Found", None, List()))
    }
  }

  "Check Translate error for subCategory not found " should {
    "return translate error" in new Setup {
      categoryRoutes.translateError(CategoryServiceError.SubCategoryNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "SubCategory Not Found", None, List()))
    }
  }

  "Check Translate error for primary category already exist " should {
    "return translate error" in new Setup {
      categoryRoutes.translateError(CategoryServiceError.PrimaryCategoryAlreadyExistError) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "Primary Category Already Exist", None, List()))
    }
  }

  "Check Translate error for subCategory already exist " should {
    "return translate error" in new Setup {
      categoryRoutes.translateError(CategoryServiceError.SubCategoryAlreadyExistError) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "subCategory Already Exist", None, List()))
    }
  }

  "Check Translate error for primary category delete error  " should {
    "return translate error" in new Setup {
      categoryRoutes.translateError(CategoryServiceError.PrimaryCategoryDeleteError) shouldBe
          Tuple2(
            StatusCodes.BadRequest,
            ErrorResponse(
              400,
              "All sub categories should be deleted prior to deleting the primary category",
              None,
              List()
            )
          )
    }
  }

  "Check Translate error non authorized " should {
    "return translate error" in new Setup {
      categoryRoutes.translateError(CategoryServiceError.AccessDenied) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Access Denied", None, List()))
    }
  }

}
