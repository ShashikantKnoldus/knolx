package com.knoldus.routes.slot

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.{ ErrorResponse, SuccessResponse }
import com.knoldus.routes.contract.slot.{
  CreateSlotRequest,
  CreateSlotResponse,
  GetFourMonthSlots,
  UpdateSlotRequest,
  UpdateSlotResponse
}
import com.knoldus.services.slot.SlotService
import com.knoldus.services.slot.SlotService.SlotServiceError
import com.knoldus.services.usermanagement.AuthorizationService
import play.api.libs.json.JsString

class SlotRoutes(service: SlotService, val authorizationService: AuthorizationService) extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("slots") {
      (post & entity(as[CreateSlotRequest])) { request =>
        authenticationWithBearerToken { authParams =>
          implicit val user: NewUserInformation = authParams
          onSuccess(service.createSlot(request.slotType, request.dateTime)) {
            case Right(slotInfo) =>
              complete(
                CreateSlotResponse(
                  slotInfo.id,
                  slotInfo.entity.dateTime,
                  slotInfo.entity.slotDuration,
                  slotInfo.entity.bookable,
                  slotInfo.entity.createdBy,
                  slotInfo.entity.createdOn,
                  slotInfo.entity.slotType
                )
              )
            case Left(error) => complete(translateError(error))
          }
        }
      } ~
        path(Segment) { slotId =>
          delete {
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.deleteSlot(slotId)) {
                case Right(_) => complete(SuccessResponse(true))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path(Segment) { slotId =>
          authenticationWithBearerToken { authParams =>
            (put & entity(as[UpdateSlotRequest])) { request =>
              implicit val user: NewUserInformation = authParams
              onSuccess(
                service.updateSlot(slotId, request.newSlotType, request.newDateTime)
              ) {
                case Right(_) => complete(UpdateSlotResponse(true))
                case Left(error) => complete(translateError(error))
              }
            }
          }
        } ~
        path("getFourMonths") {
          get {
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.getSlotsInMonth) { slot =>
                complete(GetFourMonthSlots.fromDomain(slot))
              }
            }
          }
        }
    }

  def translateError(error: SlotServiceError): (StatusCode, ErrorResponse) =
    error match {
      case SlotServiceError.SlotNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Slot Not Found")
      case SlotServiceError.EmailNotFoundError =>
        errorResponse(StatusCodes.BadRequest, "The fields in the request body are not valid")
      case SlotServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
      case SlotServiceError.InvalidSlotDetails(errors) =>
        (
          StatusCodes.UnprocessableEntity,
          ErrorResponse(
            StatusCodes.UnprocessableEntity.intValue,
            "Unable to process the given entity",
            Some("Details are invalid"),
            errors.map(JsString)
          )
        )
      case SlotServiceError.SlotBookedError => errorResponse(StatusCodes.Forbidden, "Booked Slot Can't be Deleted")
      case SlotServiceError.SlotNotUpdatedError =>
        errorResponse(StatusCodes.InternalServerError, "Something went wrong!")
    }
}
