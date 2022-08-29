package com.knoldus.routes.feedbackform

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.dao.commons.WithId
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.{ ErrorResponse, IdResponse }
import com.knoldus.routes.contract.feedbackform._
import com.knoldus.services.feedbackform.FeedbackFormService
import com.knoldus.services.feedbackform.FeedbackFormService.{ FeedbackFormServiceError, FeedbackServiceError }
import com.knoldus.services.usermanagement.AuthorizationService

class FeedbackFormRoutes(service: FeedbackFormService, val authorizationService: AuthorizationService)
    extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("feedback-form") {
      path("list-all") {
        (get & parameters("pageNumber".as[Int])) { pageNumber =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            onSuccess(service.listAllFeedbackForms(pageNumber)) {
              case Right((forms, count)) => complete(FeedbackFormsList.fromDomain(forms, pageNumber, count))
              case Left(error) => complete(translateError(error))
            }
          }
        }
      } ~
        path("create-feedback-form") {
          (post & entity(as[FeedbackFormInformation])) { request =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(
                service.createFeedbackForm(
                  request.name,
                  request.questions
                )
              ) {
                case Right(form) => complete(FeedbackFormResponse.fromDomain(form))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("all-forms") {
          get {
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.getAllFeedbackForm) {
                case Right(feedbackFormData) => complete(feedbackFormData)
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path(Segment) { id =>
          get {
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.getFeedbackFormById(id)) {
                case Right(feedbackFormData) =>
                  complete(FeedbackFormData.fromDomain(WithId(feedbackFormData, id)))
                case Left(error) => complete(translateError(error))
              }
            }
          } ~
            (put & entity(as[UpdateFeedbackFormRequest])) { request =>
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                onSuccess(
                  service.update(
                    id,
                    request.name,
                    request.questions
                  )
                ) {
                  case Right(_) => complete(IdResponse(id))
                  case Left(error) => complete(translateError(error))
                }
              }
            } ~
            delete {
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                onSuccess(service.delete(id)) {
                  case Right(_) => complete(IdResponse(id))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
        }
    }

  def translateError(error: FeedbackFormServiceError): (StatusCode, ErrorResponse) =
    error match {
      case FeedbackServiceError.FeedbackError =>
        errorResponse(StatusCodes.BadRequest, "Feedback Error")
      case FeedbackServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
      case FeedbackServiceError.FeedbackNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Feedback Not Found")
    }
}
