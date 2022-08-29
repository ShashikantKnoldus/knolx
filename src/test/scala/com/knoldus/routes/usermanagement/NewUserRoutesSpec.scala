package com.knoldus.routes.usermanagement

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.usermanagement.NewUser
import com.knoldus.services.usermanagement.NewUserService.NewUserServiceError
import com.knoldus.services.usermanagement.{ AuthorizationService, NewUserService }
import org.keycloak.representations.idm.UserRepresentation
import org.mockito.Mockito.when

class NewUserRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val newUserService: NewUserService = mock[NewUserService]
    val userRepresentation: UserRepresentation = mock[UserRepresentation]
    val authorizationService: AuthorizationService = mock[AuthorizationService]
    val newUser: NewUser = NewUser("something", "something")
    val userList: List[NewUser] = List(newUser)
    val routes: Route = new NewUserRoutes(newUserService, authorizationService).routes
    val newUserRoutes = new NewUserRoutes(newUserService, authorizationService)
    val error: NewUserServiceError = mock[NewUserServiceError]

  }
  "GET/ get all users" should {

    "return all user's data" in new Setup {
      when(newUserService.getAllUsers) thenReturn future(userList)
      Get(s"/users").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error" in new Setup {
      when(newUserService.getAllUsers) thenThrow classOf[NullPointerException]
      Get(s"/users").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Check Translate error for User Not Found" should {
    "return translate error" in new Setup {
      newUserRoutes.translateError(NewUserServiceError.UserNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "User Not Found", None, List()))
    }
  }

  "Check Translate error for Internal Server Error" should {
    "return translate error" in new Setup {
      newUserRoutes.translateError(NewUserServiceError.InternalServerError) shouldBe
          Tuple2(StatusCodes.InternalServerError, ErrorResponse(500, "Internal Server Error", None, List()))
    }
  }
}
