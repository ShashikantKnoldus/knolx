package com.knoldus.routes.session

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.common.{ ErrorResponse, SuccessResponse }
import com.knoldus.routes.{ AuthorizationRoutes, BaseRoutes }
import com.knoldus.routes.contract.session._
import com.knoldus.services.session.NewSessionService
import com.knoldus.services.session.NewSessionService.NewSessionServiceError
import com.knoldus.services.usermanagement.AuthorizationService

class NewSessionRoutes(val service: NewSessionService, val authorizationService: AuthorizationService)
    extends BaseRoutes
    with AuthorizationRoutes {

  val routes: Route =
    pathPrefix("sessions") {
      pathEnd {
        (get & parameters("pageNumber".as[Int], "pageSize".as[Int], "filter".as[String], "search".?)) {
          (pageNumber, pageSize, filter, search) =>
            filter match {
              case "upcoming" =>
                onSuccess(service.getUpcomingSessions(pageNumber, pageSize, search)) {
                  case Right(sessionsWithCount) =>
                    val (sessions, count) = sessionsWithCount
                    complete(SessionStatusResponse.fromDomain(sessions, count, pageSize))
                  case Left(error) => complete(translateError(error))
                }
              case "past" =>
                onSuccess(service.getPastSessions(pageNumber, pageSize, search)) {
                  case Right(sessionsWithCount) =>
                    val (sessions, count) = sessionsWithCount
                    complete(SessionStatusResponse.fromDomain(sessions, count, pageSize))
                  case Left(error) => complete(translateError(error))
                }
              case _ =>
                complete(translateError(NewSessionServiceError.FilterNotFound))
            }
        }
      } ~
        path("manage") {
          (get & parameters("pageNumber".as[Int], "pageSize".as[Int], "filter".as[String], "search".?)) {
            (pageNumber, pageSize, filter, search) =>
              authenticationWithBearerToken { userInfo =>
                implicit val userDetails: NewUserInformation = userInfo
                filter match {
                  case "requested" =>
                    onSuccess(service.getRequestedSessions(pageNumber, pageSize, search)) {
                      case Right(sessionsWithCount) =>
                        val (sessions, count) = sessionsWithCount
                        complete(ManageSessionsResponse.fromDomain(sessions, count, pageSize))
                      case Left(error) => complete(translateError(error))
                    }
                  case "upcoming" =>
                    onSuccess(service.getApprovedSessions(pageNumber, pageSize, search)) {
                      case Right(sessionsWithCount) =>
                        val (sessions, count) = sessionsWithCount
                        complete(ManageSessionsResponse.fromDomain(sessions, count, pageSize))
                      case Left(error) => complete(translateError(error))
                    }
                }
              }
          }
        } ~
        path("bookSession") {
          (post & entity(as[BookSessionUserRequest])) { request =>
            authenticationWithBearerToken { userInfo =>
              implicit val userDetails: NewUserInformation = userInfo
              onSuccess(service.bookSession(request)) {
                case Right(_) => complete(SuccessResponse(true))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("tags" / Segment) { keyword =>
          get
          onSuccess(service.fetchSessionTags(keyword)) {
            case Right(tags) => complete(TagsResponse(tags))
            case Left(error) => complete(translateError(error))
          }
        } ~
        path("update-session" / Segment) { id =>
          (put & entity(as[UpdateSessionUserRequest])) { request =>
            authenticationWithBearerToken { authParams =>
              implicit val userDetails: NewUserInformation = authParams
              onSuccess(
                service.updateNewSessions(
                  id,
                  request.coPresenterDetails,
                  request.dateTime,
                  request.topic,
                  request.category,
                  request.subCategory,
                  request.feedbackFormId,
                  request.sessionType,
                  request.sessionDescription
                )
              ) {
                case Right(_) => complete(UpdateSuccessResponse(id))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("my") {
          (get & parameters("pageNumber".as[Int], "pageSize".as[Int], "filter".as[String])) {
            (pageNumber, pageSize, filter) =>
              authenticationWithBearerToken { userDetails =>
                implicit val user: NewUserInformation = userDetails
                onSuccess(service.getUserSessions(pageNumber, pageSize, filter)) {
                  case Right(sessionsWithCount) =>
                    val (sessions, count) = sessionsWithCount
                    complete(UserSessionsResponse.fromDomain(sessions, count))
                  case Left(error) => complete(translateError(error))
                }
              }
          }
        }
    } ~
        path("getSession" / Segment) { id =>
          get {
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.getNewSession(id)) {
                case Right(session) => complete(GetSessionByIdNewResponse.fromDomain(session))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("user" / "session" / Segment) { id =>
          (put & entity(as[UpdateUserSession])) { request =>
            authenticationWithBearerToken { authParams =>
              implicit val userDetails: NewUserInformation = authParams
              onSuccess(
                service.updateUserSession(
                  id,
                  topic = request.topic,
                  sessionDescription = request.sessionDescription,
                  slideURL = request.slideURL,
                  sessionTags = request.sessionTag
                )
              ) {

                case Right(_) => complete(UpdateSuccessResponse(id))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        }

  def translateError(error: NewSessionServiceError): (StatusCode, ErrorResponse) =
    error match {
      case NewSessionServiceError.InternalServerError =>
        errorResponse(StatusCodes.InternalServerError, "Internal Server Error")
      case NewSessionServiceError.SlotNotAvailable =>
        errorResponse(StatusCodes.NotFound, "Slot Not Available")
      case NewSessionServiceError.InvalidSlotId(id) =>
        errorResponse(StatusCodes.NotFound, s"Invalid slot id provided: $id")
      case NewSessionServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
      case NewSessionServiceError.SessionNotFound =>
        errorResponse(StatusCodes.NotFound, "Session Not Found")
      case NewSessionServiceError.FilterNotFound =>
        errorResponse(StatusCodes.NotFound, "Request is missing required query parameter 'filter'")
      case NewSessionServiceError.NotFound =>
        errorResponse(StatusCodes.NotFound, "Cannot Fetch Tags")
      case NewSessionServiceError.MandatoryFieldsNotFound =>
        errorResponse(StatusCodes.NotFound, "Mandatory fields not given in the request")
      case NewSessionServiceError.InvalidRequest =>
        errorResponse(StatusCodes.BadRequest, "The fields in the request body are not valid")
    }
}
