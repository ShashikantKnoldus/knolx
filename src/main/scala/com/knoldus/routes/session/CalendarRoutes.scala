package com.knoldus.routes.session

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.{ ErrorResponse, IdResponse }
import com.knoldus.routes.contract.session._
import com.knoldus.services.session.CalendarService
import com.knoldus.services.session.CalendarService.CalendarServiceError
import com.knoldus.services.usermanagement.AuthorizationService

import java.util.Date

class CalendarRoutes(service: CalendarService, val authorizationService: AuthorizationService)
    extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("calendar") {
      pathEnd {
        (get & parameters("pageNumber".as[Int], "pageSize".as[Int], "email".?)) { (pageNumber, pageSize, email) =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams

            if (email.isDefined)
              onSuccess(service.getAllSessionsForAdmin(pageNumber, pageSize, email)) {
                case Right(sessions) =>
                  val (session, count) = sessions
                  complete(AllSessionResponse.fromDomain(session, count, pageSize))
                case Left(error) => complete(translateError(error))
              }
            else
              onSuccess(service.getAllSessionsForAdmin(pageNumber, pageSize)) {
                case Right(sessions) =>
                  val (session, count) = sessions
                  complete(AllSessionResponse.fromDomain(session, count, pageSize))
                case Left(error) => complete(translateError(error))
              }
          }
        } ~
          (get & parameters("startDate".as[Long], "endDate".as[Long])) { (startDate, endDate) =>
            onSuccess(service.getSessionsInMonth(startDate, endDate)) {
              case Right(sessions) => complete(GetCalendarSessionsInMonthResponse.fromDomain(sessions))
              case Left(error) => complete(translateError(error))
            }
          } ~
          get {
            onSuccess(service.getPendingSessions) {
              case Right(count) => complete(count)
              case Left(error) => complete(translateError(error))
            }
          }
      } ~
        path("email") {
          (post & entity(as[EmailRequest])) { request =>
            onSuccess(service.sendEmail(request.sessionId)) {
              case Right(response) => complete(response)
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path("get-session") {
          (get & parameters("id".as[String])) { id =>
            authenticationWithBearerToken { _ =>
              onSuccess(service.getSessionById(id)) {
                case Right(session) => complete(GetSessionRequestByUserResponse.fromDomain(session))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("update-session-request") {
          parameters("id".as[String]) { id =>
            (put & entity(as[UpdateApproveSessionInfo])) { request =>
              authenticationWithBearerToken { _ =>
                onSuccess(service.updateSessionForApprove(id, request)) {
                  case Right(_) => complete(IdResponse(id))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
          }
        } ~
        path("update-date") {
          parameters("id".as[String]) { id =>
            (put & entity(as[Date])) { request =>
              authenticationWithBearerToken { _ =>
                onSuccess(service.updateDateForPendingSession(id, request)) {
                  case Right(_) => complete(IdResponse(id))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
          }
        } ~
        path("decline-session") {
          parameters("id".as[String]) { id =>
            (post & entity(as[DeclineSessionRequest])) { request =>
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                onSuccess(service.declineSession(id, request)) {
                  case Right(session) => complete(GetSessionRequestByUserResponse.fromDomain(session))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
          }
        }
    } ~
        pathPrefix("freeslot") {
          pathEnd {
            get {
              onSuccess(service.getAllFreeSlots) {
                case Right(freeSlots) => complete(SessionRequestInfo.fromDomain(freeSlots))
                case Left(error) => complete(translateError(error))
              }
            } ~
              (post & entity(as[AddSlotRequest])) { request =>
                authenticationWithBearerToken { authParams =>
                  implicit val user: NewUserInformation = authParams
                  onSuccess(service.insertSlot(request)) {
                    case Right(result) => complete(AddSlotResponse.fromDomain(result))
                    case Left(error) => complete(translateError(error))
                  }
                }
              }
          } ~
            path(Segment) { id =>
              delete {
                authenticationWithBearerToken { _ =>
                  onSuccess(service.deleteSlot(id)) {
                    case Right(_) => complete(IdResponse(id))
                    case Left(error) => complete(translateError(error))
                  }
                }
              }
            }
        }

  def translateError(error: CalendarServiceError): (StatusCode, ErrorResponse) =
    error match {
      case CalendarServiceError.SlotNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Slot Not Found")
      case CalendarServiceError.SessionNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Session Not Found")
      case CalendarServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
    }
}
