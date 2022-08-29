package com.knoldus.routes.contract.slot

import play.api.libs.json.{ Json, OWrites }

case class CreateSlotResponse(
  slotId: String,
  dateTime: Long,
  slotDuration: Int,
  bookable: Boolean,
  createdBy: String,
  createdOn: Long,
  slotType: String
)

object CreateSlotResponse {
  implicit val createSlotResponseWrites: OWrites[CreateSlotResponse] = Json.writes[CreateSlotResponse]
}
