package com.knoldus.routes.session

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.{ ErrorResponse, IdResponse }
import com.knoldus.routes.contract.session._
import com.knoldus.services.session.SessionService
import com.knoldus.services.session.SessionService.SessionServiceError
import com.knoldus.services.usermanagement.AuthorizationService

class SessionRoutes(service: SessionService, val authorizationService: AuthorizationService)
    extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("session") {
      path("email") {
        (post & entity(as[EmailRequest])) { request =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            onSuccess(service.sendEmailToPresenter(request.sessionId)) {
              case Right(response) => complete(response)
              case Left(error) => complete(translateError(error))
            }
          }
        }
      } ~
        path(Segment) { id =>
          get {

            onSuccess(service.getSession(id)) {
              case Right(session) => complete(GetSessionByIdResponse.fromDomain(session))
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path("delete-session") {
          (delete & parameter("id".as[String])) { id =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.delete(id)) {
                case Right(_) => complete(IdResponse(id))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("update-session") {
          parameters("id".as[String]) { id =>
            (put & entity(as[UpdateSessionRequest])) { request =>
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                onSuccess(
                  service.update(
                    id,
                    request.session,
                    request.date,
                    request.category,
                    request.subCategory,
                    request.feedbackFormId,
                    request.topic,
                    request.brief,
                    request.feedbackExpirationDays,
                    request.youtubeURL,
                    request.slideShareURL,
                    request.cancelled,
                    request.meetup
                  )
                ) {
                  case Right(_) => complete(IdResponse(id))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
          }
        } ~
        path("schedule") {
          (get & parameters("sessionId".as[String])) { sessionId =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.scheduleSession(sessionId)) {
                case Right(result) => complete(result)
                case Left(error) => complete(translateError(error))
              }
            }
          }
        }
    } ~
        path("getUpcomingSessions") {
          (get & parameter("email".?)) { email =>
            onSuccess(service.getUpcomingSessions(email)) {
              case Right(sessions) =>
                complete(UpcomingSessionsResponse.fromDomain(sessions))
              case Left(error) =>
                complete(translateError(error))
            }
          }
        } ~
        pathPrefix("video-upload") {
          path("storeTemporaryURL") {
            (get & parameters("sessionId".as[String], "videoURL".as[String])) { (sessionId, videoURL) =>
              onSuccess(service.storeTemporaryURL(sessionId, videoURL)) {
                case Right(result) => complete(result)
                case Left(error) => complete(translateError(error))
              }
            }
          } ~
            path("getTemporaryURL") {
              (get & parameters("sessionId".as[String])) { sessionId =>
                onSuccess(service.getTemporaryVideoURL(sessionId)) {
                  case Right(result) => complete(List(result))
                  case Left(error) => complete(translateError(error))
                }
              }
            } ~
            path("getYoutubeURL") {
              (get & parameters("sessionId".as[String])) { sessionId =>
                onSuccess(service.getYoutubeVideoURL(sessionId)) {
                  case Right(result) => complete(List(result))
                  case Left(error) => complete(translateError(error))
                }
              }
            } ~
            path("check-Authorization") {
              get {
                authenticationWithBearerToken { _ =>
                  complete("Authorized")
                }
              }
            }
        }

  def translateError(error: SessionServiceError): (StatusCode, ErrorResponse) =
    error match {

      case SessionServiceError.SessionNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Session Not Found")
      case SessionServiceError.EmailNotFoundError =>
        errorResponse(StatusCodes.BadRequest, "Email Not Valid")
      case SessionServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
    }
}
