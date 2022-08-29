package com.knoldus.routes.usermanagement

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.usermanagement._
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.usermanagement.UserManagementService.UserManagementServiceError
import com.knoldus.services.usermanagement.{ AuthorizationService, UserManagementService }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{ JsValue, Json }

import java.util.Date
import scala.concurrent.Future

class UserManagementRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val userManagementService: UserManagementService = mock[UserManagementService]
    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val userManagementRoutes = new UserManagementRoutes(userManagementService, cache, authorizationService)
    val testEmail = "test"
    val testName = "Testing"
    val testPassword = "test1234"
    val testToken = "test"
    val pageNumber = 1
    val pageSize = 10
    val filter = "all"
    val routes: Route = new UserManagementRoutes(userManagementService, cache, authorizationService).routes
    val error: UserManagementServiceError = mock[UserManagementServiceError]
    val userStatusInformationResponse = new UserStatusInformationResponse(testEmail, true, true, false, true, None)
    val updateUserStatusRequest = new UpdateUserStatusRequest(testEmail, true, true, false, true, None)
    val validationDetails: KeycloakDetails = KeycloakDetails(testEmail, Admin)

    val searchUserResponse: SearchUserResponse =
      SearchUserResponse(
        "test",
        active = true,
        "id",
        "test",
        admin = false,
        superUser = false,
        coreMember = false,
        ban = false,
        None
      )

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
        None,
        nonParticipating = false,
        None
      )

    val withIdUser: WithId[UserInformation] = WithId(entity = userInformation, "1")

    implicit val userInformationResponse: UserInformationResponse = UserInformationResponse(
      testEmail,
      active = true,
      admin = true,
      coreMember = true,
      superUser = true,
      new Date(),
      1,
      "id",
      None
    )

    val userToken: UserToken = UserToken("token", new Date(), userInformation)
    val dateTimeUtility = new DateTimeUtility
    val userMail = "userMail"
    val date = new Date(dateTimeUtility.toMillis(dateTimeUtility.localDateTimeIST.plusDays(1)))
    cache.remove("token")
    val result: Future[UserToken] = cache.getOrLoad("token", _ => Future(UserToken("token", date, userInformation)))

  }

  "GET/ get user status endpoint" should {

    "return user's status" in new Setup {
      userManagementService.getUserStatusByEmail(testEmail) shouldReturn Future(Right(userStatusInformationResponse))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/getuserstatusbyemail?email=$testEmail")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error" in new Setup {
      userManagementService.getUserStatusByEmail(any[String]) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/getuserstatusbyemail?email=$testEmail")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "GET/ delete user endpoint" should {

    "return user's status" in new Setup {
      userManagementService.deleteUser(any[String]) shouldReturn Future(Right("Deleted"))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/delete?email=$testEmail").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error" in new Setup {
      userManagementService.deleteUser(any[String]) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/delete?email=$testEmail").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST/ update user status endpoint" should {

    "update user's status" in new Setup {
      userManagementService.updateUserStatus(any[UpdateUserStatusRequest]) shouldReturn Future(Right("Updated"))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"email":"test",
                                       |"active":true,
                                       |"ban":true,
                                       |"coreMember": false,
                                       |"admin": true,
                                       |"password": "{None}"
                                       |}
                            """.stripMargin)
      Post(s"/manageuser/updateuserstatus", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error" in new Setup {
      userManagementService.updateUserStatus(any[UpdateUserStatusRequest]) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"email":"test",
                                       |"active":true,
                                       |"ban":true,
                                       |"coreMember": false,
                                       |"admin": true,
                                       |"password": "{None}"
                                       |}
                            """.stripMargin)
      Post(s"/manageuser/updateuserstatus", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST/ update user department endpoint" should {
    "update user's department" in new Setup {
      when(
        userManagementService.updateUserDepartment(any[String], any[String])(any[NewUserInformation])
      ) thenReturn Future(Right(Done))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val request: JsValue = Json.parse("""{
                                          | "email": "test",
                                          | "newDepartment": "Scala"
                                          |}""".stripMargin)
      Post(s"/manageuser/update-user-department", request).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error in updating user's department" in new Setup {
      userManagementService.updateUserDepartment(testEmail, "Scala") shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"email":"test",
                                       |"newDepartment":"Scala"
                                       |}
                            """.stripMargin)
      Post(s"/manageuser/update-user-department", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "GET/ get users list" should {

    "return users when email id not provided" in new Setup {
      userManagementService.usersList(any[Option[String]]) shouldReturn Future(Right(List(testEmail)))

      Get(s"/user/userslist").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error" in new Setup {
      userManagementService.usersList(any[Option[String]]) shouldReturn Future(Left(error))

      Get(s"/user/userslist?email=$testEmail").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "POST/ update participant status endpoint" should {

      "change participant status" in new Setup {
        userManagementService.changeParticipantStatus(
          testEmail,
          any[ChangeParticipantStatusResponse]
        ) shouldReturn Future(Right(akka.Done))
        when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
        when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
        val body: JsValue = Json.parse("""
                                         |{
                                         |"email":"test",
                                         |"nonParticipating": false
                                         |}
                            """.stripMargin)
        Post(s"/user/update-participant-status", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
          status shouldBe StatusCodes.OK
        }
      }

      "return error" in new Setup {
        userManagementService.changeParticipantStatus(
          testEmail,
          any[ChangeParticipantStatusResponse]
        ) shouldReturn Future(Left(error))
        when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
        when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
        val body: JsValue = Json.parse("""
                                         |{
                                         |"email":"test",
                                         |"nonParticipating": false
                                         |}
                            """.stripMargin)
        Post(s"/user/update-participant-status", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
          status shouldBe StatusCodes.InternalServerError
        }
      }
    }

    "return users when email id is provided" in new Setup {
      userManagementService.usersList(any[Option[String]]) shouldReturn Future(Right(List(testEmail)))

      Get(s"/user/userslist?email=$testEmail").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return users information by email" in new Setup {
      userManagementService.getUserInformationByEmail(any[String]) shouldReturn Future(Right(userInformationResponse))

      Get(s"/user/email").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error when email not found" in new Setup {
      userManagementService.getUserInformationByEmail(any[String]) shouldReturn Future(Left(error))

      Get(s"/user/email").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "return get Active And UnBanned Emails" in new Setup {
      userManagementService.getActiveAndUnBannedEmails shouldReturn Future(Right(List("abc@knoldus.com")))

      Get(s"/user/allactiveunbanemails").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error when get Active And UnBanned Emails not found" in new Setup {
      userManagementService.getActiveAndUnBannedEmails shouldReturn Future(
            Left(UserManagementServiceError.UserNotFoundError)
          )

      Get(s"/user/allactiveunbanemails").check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return ban users by email" in new Setup {
      userManagementService.banUser(any[String]) shouldReturn Future(true)

      Get(s"/user/banuser?email=$testEmail").check {
        assert(true)
      }
    }
  }

  "GET/ get users " should {

    "return users when email id not provided" in new Setup {
      userManagementService.searchUsers(any[Int], any[Int], any[String], any[Option[String]]) shouldReturn Future(
            Right((Seq(searchUserResponse), 1))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/search?pageNumber=$pageNumber&pageSize=$pageSize&filter=$filter")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error email id not provided" in new Setup {
      userManagementService.searchUsers(any[Int], any[Int], any[String], any[Option[String]]) shouldReturn Future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/search?pageNumber=$pageNumber&pageSize=$pageSize&filter=$filter")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
    "return users when email id is provided" in new Setup {
      userManagementService.searchUsers(any[Int], any[Int], any[String], any[Option[String]]) shouldReturn Future(
            Right((Seq(searchUserResponse), 1))
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/search?pageNumber=$pageNumber&pageSize=$pageSize&filter=$filter&email=$testEmail")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error email id is provided" in new Setup {
      userManagementService.searchUsers(any[Int], any[Int], any[String], any[Option[String]]) shouldReturn Future(
            Left(error)
          )
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/manageuser/search?pageNumber=$pageNumber&pageSize=$pageSize&filter=$filter&email=$testEmail")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Check Translate error for User Not Found Error " should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.UserNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Mail Not Found"))
    }
  }

  "Check Translate error for Email Already Exists " should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.EmailAlreadyExist) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "Email Already Exists", None, List()))
    }
  }

  "Check Translate error for Invalid Credential" should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.InvalidCredentials) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Invalid Credentials", None, List()))
    }
  }

  "Check Translate error for Token Expired" should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.TokenExpired) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Token Expired", None, List()))
    }
  }

  "Check Translate error for No Forgot Password Request Found" should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.NoForgotPasswordRequestFound) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "NoForgotPasswordRequestFound", None, List()))
    }
  }

  "Check Translate error for Access Denied" should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.AccessDenied) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Access Denied", None, List()))
    }
  }

  "Check Translate error for Account deactivated" should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.UserDeactivated) shouldBe
          Tuple2(StatusCodes.BadRequest, ErrorResponse(400, "User Deactivated", None, List()))
    }
  }

  "Check Translate error for account not activated" should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.UserNotActivated) shouldBe
          Tuple2(StatusCodes.NotAcceptable, ErrorResponse(406, "User Not Activated", None, List()))
    }
  }

  "Check Translate error for Unauthorize Credential" should {
    "return translate error" in new Setup {
      userManagementRoutes.translateError(UserManagementServiceError.Unauthorized) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "User Not Unauthorized", None, List()))
    }
  }

}
