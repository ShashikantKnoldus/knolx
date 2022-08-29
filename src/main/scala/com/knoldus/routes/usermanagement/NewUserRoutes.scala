package com.knoldus.routes.usermanagement

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.usermanagement.NewUsersResponse
import com.knoldus.services.usermanagement.{ AuthorizationService, NewUserService }
import com.knoldus.services.usermanagement.NewUserService.NewUserServiceError

import scala.concurrent.ExecutionContext

class NewUserRoutes(service: NewUserService, val authorizationService: AuthorizationService)(implicit
  ec: ExecutionContext,
  logger: LoggingAdapter
) extends AuthorizationRoutes {

  val routes: Route =
    path("users") {
      get {
        onSuccess(service.getAllUsers) { users =>
          complete(NewUsersResponse.fromDomain(users))
        }
      }
    } ~
        path("allUsers") {
          get {
            onSuccess(service.getUsers) { users =>
              complete(NewUsersResponse.fromDomain(users))
            }
          }
        }

  def translateError(error: NewUserServiceError): (StatusCode, ErrorResponse) =
    error match {
      case NewUserServiceError.UserNotFoundError =>
        errorResponse(StatusCodes.NotFound, "User Not Found")

      case NewUserServiceError.InternalServerError =>
        errorResponse(StatusCodes.InternalServerError, "Internal Server Error")
    }

}
