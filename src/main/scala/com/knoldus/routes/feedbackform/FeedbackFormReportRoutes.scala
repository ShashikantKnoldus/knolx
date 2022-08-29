package com.knoldus.routes.feedbackform

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.feedbackform.{ FeedbackReport, ReportResult }
import com.knoldus.services.feedbackform.FeedbackFormReportService
import com.knoldus.services.feedbackform.FeedbackFormReportService.FeedbackFormReportServiceError
import com.knoldus.services.usermanagement.AuthorizationService

class FeedbackFormReportRoutes(
  service: FeedbackFormReportService,
  val authorizationService: AuthorizationService
) extends AuthorizationRoutes {

  val routes: Route = {
    pathPrefix("knolx-reports") {
      path(Segment) { userId =>
        (get & parameters("pageNumber".as[Int])) { pageNumber =>
          authenticationWithBearerToken { _ =>
            onSuccess(service.manageUserFeedbackReports(userId, pageNumber)) {
              case Right(reportResult) => complete(ReportResult.fromDomain(reportResult))
              case Left(error) => complete(translateError(error))
            }
          }
        }
      } ~
        path("fetch-user-response-by-session") {
          (get & parameters("userId".as[String], "sessionId".as[String])) { (userId, sessionId) =>
            authenticationWithBearerToken { _ =>
              onSuccess(service.fetchUserResponsesBySessionId(sessionId, userId)) {
                case Right(feedbackReport) => complete(FeedbackReport.fromDomain(feedbackReport))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        }
    } ~
      pathPrefix("reports") {
        path("all-feedback-reports") {
          (get & parameters("pageNumber".as[Int])) { pageNumber =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.manageAllFeedbackReports(pageNumber)) {
                case Right(reportResult) => complete(ReportResult.fromDomain(reportResult))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
          path(Segment) { userId =>
            (get & parameters("sessionId".as[String])) { sessionId =>
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                onSuccess(service.fetchAllResponsesBySessionId(sessionId, userId)) {
                  case Right(feedbackReport) =>
                    complete(FeedbackReport.fromDomain(feedbackReport))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
          }
      }
  }

  def translateError(error: FeedbackFormReportServiceError): (StatusCode, ErrorResponse) =
    error match {
      case FeedbackFormReportServiceError.FeedbackError =>
        errorResponse(StatusCodes.BadRequest, "Feedback Report Error")
      case FeedbackFormReportServiceError.FeedbackAccessDeniedError =>
        errorResponse(StatusCodes.Unauthorized, " Access Denied Error")
      case FeedbackFormReportServiceError.FeedbackNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Feedback Report Not Found")
      case FeedbackFormReportServiceError.UserNotFound =>
        errorResponse(StatusCodes.NotFound, "User Not Found ")
    }
}
