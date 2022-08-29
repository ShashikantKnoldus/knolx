package com.knoldus.routes.feedbackform

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.dao.commons.WithId
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.feedbackform._
import com.knoldus.services.feedbackform.FeedbackFormResponseService
import com.knoldus.services.feedbackform.FeedbackFormResponseService.FeedbackFormResponseServiceError
import com.knoldus.services.usermanagement.AuthorizationService

class FeedbackFormResponseRoutes(
  service: FeedbackFormResponseService,
  val authorizationService: AuthorizationService
) extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("feedbacks") {
      path("bannedusers") {
        (get & parameters("userId".as[String])) { userId =>
          authenticationWithBearerToken { _ =>
            onSuccess(service.getBanUserDetails(userId)) {
              case Right(user) => complete(BannedUser.fromDomain(user))
              case Left(error) => complete(translateError(error))
            }
          }
        }
      } ~
        path("store-feedback") {
          parameters("userId".as[String]) { userId =>
            (post & entity(as[FeedbackResponse])) { request =>
              authenticationWithBearerToken { _ =>
                onSuccess(
                  service.storeFeedbackResponse(
                    userId,
                    request.sessionId,
                    request.feedbackFormId,
                    request.responses,
                    request.score
                  )
                ) {
                  case Right(userFeedbackResponse) =>
                    complete(SessionFeedbackResponse.fromDomain(WithId(userFeedbackResponse, request.sessionId)))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
          }
        } ~
        path("getFeedbackFormForToday") {
          (get & parameters("userId".as[String])) { userId =>
            authenticationWithBearerToken { _ =>
              onSuccess(service.getFeedbackFormForToday(userId)) {
                case Right(feedbackSessions) => complete(FeedbackFormsToday.fromdomain(feedbackSessions))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("fetch-feedback-response") {
          (get & parameters("userId".as[String], "sessionId".as[String])) { (sessionId, userId) =>
            authenticationWithBearerToken { _ =>
              onSuccess(service.fetchFeedbackResponse(sessionId, userId)) {
                case Right(storedResponse) => complete(FeedbackFormFetchResponse.fromDomain(storedResponse))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        }
    }

  def translateError(error: FeedbackFormResponseServiceError): (StatusCode, ErrorResponse) =
    error match {
      case FeedbackFormResponseServiceError.FeedbackError =>
        errorResponse(StatusCodes.BadRequest, "Feedback Response Error")
      case FeedbackFormResponseServiceError.FeedbackAccessDeniedError =>
        errorResponse(StatusCodes.Unauthorized, " Access Denied Error")
      case FeedbackFormResponseServiceError.FeedbackNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Feedback Response Not Found")
    }
}
