package com.knoldus.routes.usermanagement

import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.{ NewUserInformation, UserToken }
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.{ ErrorResponse, SuccessResponse }
import com.knoldus.routes.contract.usermanagement._
import com.knoldus.services.usermanagement.UserManagementService.UserManagementServiceError
import com.knoldus.services.usermanagement.{ AuthorizationService, UserManagementService }

import scala.concurrent.ExecutionContext.global
import scala.concurrent._

class UserManagementRoutes(
  service: UserManagementService,
  cache: Cache[String, UserToken],
  val authorizationService: AuthorizationService
) extends AuthorizationRoutes {
  implicit val executionContext: ExecutionContextExecutor = global

  val routes: Route =
    pathPrefix("user") {

      path("allactiveunbanemails") {
        get {
          onSuccess(service.getActiveAndUnBannedEmails) {
            case Right(response) => complete(response)
            case Left(error) => complete(translateError(error))
          }
        }
      } ~
        path("update-participant-status") {
          (post & entity(as[ChangeParticipantStatusResponse])) { modifyParticipantStatus =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.changeParticipantStatus(modifyParticipantStatus.email, modifyParticipantStatus)) {
                case Left(error) =>
                  complete(translateError(error))
                case _ =>
                  complete(SuccessResponse(true))
              }
            }
          }
        } ~
        path("banuser") {
          (get & parameters("email".as[String])) { email =>
            complete(service.banUser(email))
          }
        } ~
        path("userslist") {
          (get & parameters("email".?)) { email =>
            onSuccess(service.usersList(email)) {
              case Right(response) => complete(response)
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path(Segment) { email =>
          get {
            onSuccess(service.getUserInformationByEmail(email)) {
              case Right(response) => complete(response)
              case Left(error) => complete(translateError(error))
            }
          }
        }
    } ~
        pathPrefix("manageuser") {
          path("search") {
            (get & parameters(
                  "pageNumber".as[Int],
                  "pageSize".as[Int],
                  "filter".as[String],
                  "email".?
                )) { (pageNumber, pageSize, filter, email) =>
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                onSuccess(service.searchUsers(pageNumber, pageSize, filter, email)) {
                  case Right(response) => complete(response)
                  case Left(error) => complete(translateError(error))
                }
              }
            }
          } ~
            path("delete") {
              (get & parameters("email".as[String])) { email =>
                authenticationWithBearerToken { authParams =>
                  implicit val user: NewUserInformation = authParams
                  onSuccess(service.deleteUser(email)) {
                    case Right(response) => complete(response)
                    case Left(error) => complete(translateError(error))
                  }
                }
              }
            } ~
            path("getuserstatusbyemail") {
              (get & parameters("email".as[String])) { email =>
                authenticationWithBearerToken { authParams =>
                  implicit val user: NewUserInformation = authParams
                  onSuccess(service.getUserStatusByEmail(email)) {
                    case Right(response) => complete(response)
                    case Left(error) => complete(translateError(error))
                  }
                }
              }
            } ~
            path("updateuserstatus") {
              (post & entity(as[UpdateUserStatusRequest])) { updateUserStatusRequest =>
                authenticationWithBearerToken { authParams =>
                  implicit val user: NewUserInformation = authParams
                  onSuccess(service.updateUserStatus(updateUserStatusRequest)) {
                    case Right(response) => complete(response)
                    case Left(error) => complete(translateError(error))
                  }
                }
              }
            } ~
            path("update-user-department") {
              (post & entity(as[UserDepartmentUpdateRequest])) { updateDepartmentRequest =>
                authenticationWithBearerToken { authParams =>
                  implicit val user: NewUserInformation = authParams
                  onSuccess(
                    service.updateUserDepartment(authParams.email, updateDepartmentRequest.newDepartment)
                  ) {
                    case Right(_) => complete(SuccessResponse(true))
                    case Left(error) => complete(translateError(error))
                  }
                }
              }
            }
        }

  def translateError(error: UserManagementServiceError): (StatusCode, ErrorResponse) =
    error match {
      case UserManagementServiceError.InternalServerError =>
        errorResponse(StatusCodes.InternalServerError, "Internal Server Error")
      case UserManagementServiceError.UserNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Mail Not Found")
      case UserManagementServiceError.EmailAlreadyExist =>
        errorResponse(StatusCodes.BadRequest, "Email Already Exists")
      case UserManagementServiceError.InvalidCredentials =>
        errorResponse(StatusCodes.Unauthorized, "Invalid Credentials")
      case UserManagementServiceError.TokenExpired =>
        errorResponse(StatusCodes.Unauthorized, "Token Expired")
      case UserManagementServiceError.NoForgotPasswordRequestFound =>
        errorResponse(StatusCodes.BadRequest, "NoForgotPasswordRequestFound")
      case UserManagementServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
      case UserManagementServiceError.UserDeactivated =>
        errorResponse(StatusCodes.BadRequest, "User Deactivated")
      case UserManagementServiceError.UserNotActivated =>
        errorResponse(StatusCodes.NotAcceptable, "User Not Activated")
      case UserManagementServiceError.Unauthorized =>
        errorResponse(StatusCodes.Unauthorized, "User Not Unauthorized")
    }

}
