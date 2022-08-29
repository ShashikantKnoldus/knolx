package com.knoldus.routes.recommendation

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.recommendation.{
  AddRecommendationResponse,
  RecommendationInformation,
  RecommendationResponse,
  RecommendationResponseInformation
}
import com.knoldus.services.recommendation.RecommendationService
import com.knoldus.services.recommendation.RecommendationService.RecommendationServiceError
import com.knoldus.services.usermanagement.AuthorizationService

class RecommendationRoutes(
  service: RecommendationService,
  val authorizationService: AuthorizationService
) extends AuthorizationRoutes {

  val routes: Route = {
    pathPrefix("recommendation") {
      pathEnd {
        (post & entity(as[RecommendationInformation])) { request =>
          onSuccess(
            service.addRecommendation(
              request
            )
          ) {
            case Right(form) => complete(AddRecommendationResponse.fromDomain(form))
            case Left(error) => complete(translateError(error))
          }
        }
      } ~
        path("list-recommendations") {
          (get & parameters("filter".as[String])) { filter =>
            onSuccess(service.listRecommendation(filter)) {
              case Right(recommendationList) => complete(recommendationList)
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path("get-vote") {
          (get & parameters("id".as[String], "email".as[String])) { (id, email) =>
            onSuccess(service.getVote(id, email)) {
              case Right(vote) => complete(vote)
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path("down-vote") {
          (put & parameters("id".as[String], "alreadyVoted".as[Boolean])) { (id, alreadyVoted) =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.downVote(id, alreadyVoted)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("update-vote") {
          (post & entity(as[RecommendationResponse])) { request =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(
                service.insertResponse(request)
              ) {
                case Right(recommendation) => complete(RecommendationResponseInformation.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("up-vote") {
          (put & parameters("recommendationId".as[String], "alreadyVoted".as[Boolean])) { (id, alreadyVoted) =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.upVote(id, alreadyVoted)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("approve" / Segment) { id =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            post {
              onSuccess(service.approveRecommendation(id)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("decline" / Segment) { id =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            post {
              onSuccess(service.declineRecommendation(id)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("pending" / Segment) { id =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            post {
              onSuccess(service.pendingRecommendation(id)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("done" / Segment) { id =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            post {
              onSuccess(service.doneRecommendation(id)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("pending-recommendations") {
          get {
            onSuccess(service.allPendingRecommendation) {
              case Right(count) => complete(count)
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path("book-recommendation" / Segment) { recommendationId =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            post {
              onSuccess(service.bookRecommendation(recommendationId)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("cancel-booked-recommendation" / Segment) { recommendationId =>
          authenticationWithBearerToken { authParams =>
            implicit val user: NewUserInformation = authParams
            post {
              onSuccess(service.cancelBookedRecommendation(recommendationId)) {
                case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path(Segment) { id =>
          get {
            onSuccess(service.getRecommendationById(id)) {
              case Right(recommendation) => complete(AddRecommendationResponse.fromDomain(recommendation))
              case Left(error) => complete(translateError(error))
            }
          }
        }
    }
  }

  def translateError(error: RecommendationServiceError): (StatusCode, ErrorResponse) =
    error match {
      case RecommendationServiceError.RecommendationError =>
        errorResponse(StatusCodes.BadRequest, "Recommendation Error")
      case RecommendationServiceError.RecommendationAccessDeniedError =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
      case RecommendationServiceError.RecommendationNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Recommendation Not Found")
      case RecommendationServiceError.InvalidRecommendationError =>
        errorResponse(StatusCodes.BadRequest, "Invalid Recommendation")
    }
}
