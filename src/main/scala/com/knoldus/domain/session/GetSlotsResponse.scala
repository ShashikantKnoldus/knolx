package com.knoldus.domain.session

import play.api.libs.json.{ Json, OWrites, Reads }

final case class GetSlotsResponse(
  id: String,
  dateTime: Long,
  bookable: Boolean,
  createdBy: String,
  sessionDetail: Option[SessionDetails],
  slotDuration: Long,
  slotType: String
)

object GetSlotsResponse {
  implicit val getSlotsResponseWrites: OWrites[GetSlotsResponse] = Json.writes[GetSlotsResponse]
  implicit val getSlotsResponseReads: Reads[GetSlotsResponse] = Json.reads[GetSlotsResponse]

}
