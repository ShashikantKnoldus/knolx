package com.knoldus.routes.session

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.session.{ MeetingRequest, MeetingResponse }
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.services.session.MeetingService.MeetingServiceError
import com.knoldus.services.session._
import com.knoldus.services.usermanagement.AuthorizationService

class MeetingRoutes(service: MeetingService, val authorizationService: AuthorizationService)
    extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("sessions") {
      path(Segment / "getLink") { sessionId =>
        (post & entity(as[MeetingRequest])) { meetingRequest =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams

            onSuccess(service.meetingLink(sessionId, meetingRequest)) {
              case Right(meetingResponse: MeetingResponse) => complete(meetingResponse)
              case Left(error) => complete(translateError(error))
            }
          }
        }
      }
    }

  /** **
    *
    * @param error
    * @return This method returned the different types of errors which are occurring during the generation of meeting link
    */
  def translateError(error: MeetingServiceError): (StatusCode, ErrorResponse) =
    error match {
      case MeetingServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
      case MeetingServiceError.SessionNotFound =>
        errorResponse(StatusCodes.NotFound, "Session Not Found")
      case MeetingServiceError.DataNotFound =>
        errorResponse(StatusCodes.BadRequest, "Data Not Found")
      case MeetingServiceError.InvalidMeetingType =>
        errorResponse(StatusCodes.NotFound, "Meeting type not correct")

    }
}
