package com.knoldus.routes.user

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.user.UserResponse
import com.knoldus.services.user.KnolxUserAnalysisService
import com.knoldus.services.user.KnolxUserAnalysisService.KnolxUserAnalysisServiceError
import com.knoldus.services.usermanagement.AuthorizationService

class KnolxUserAnalysisRoutes(
  service: KnolxUserAnalysisService,
  val authorizationService: AuthorizationService
) extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("user-analytics") {
      authenticationWithBearerToken { authParams =>
        implicit val user: NewUserInformation = authParams
        (get & parameters("email".as[String])) { email =>
          path("comparison") {
            onSuccess(service.userSessionsResponseComparison(email)) {
              case Right(response) => complete(response)
              case Left(error) => complete(translateError(error))
            }
          } ~
            path("ban-count") {
              onSuccess(service.getBanCount(email)) {
                case Right(response) => complete(UserResponse.fromDomain(response))
                case Left(error) => complete(translateError(error))
              }
            } ~
            path("total-meetups") {
              onSuccess(service.getUserTotalMeetups(email)) {
                case Right(response) => complete(response)
                case Left(error) => complete(translateError(error))
              }
            } ~
            path("not-attended") {
              onSuccess(service.getUserDidNotAttendSessionCount(email)) {
                case Right(response) => complete(response)
                case Left(error) => complete(translateError(error))
              }
            } ~
            path("total-knolx") {
              onSuccess(service.getUserTotalKnolx(email)) {
                case Right(response) => complete(response)
                case Left(error) => complete(translateError(error))
              }
            }
        }
      }
    }

  def translateError(error: KnolxUserAnalysisServiceError): (StatusCode, ErrorResponse) =
    error match {
      case KnolxUserAnalysisServiceError.UserAnalysisError =>
        errorResponse(StatusCodes.BadRequest, "User Analysis Error")
      case KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError =>
        errorResponse(StatusCodes.Unauthorized, " Access Denied Error")
      case KnolxUserAnalysisServiceError.UserAnalysisNotFoundError =>
        errorResponse(StatusCodes.NotFound, "User Analysis Report Not Found")

    }
}
