package com.knoldus.routes.contract.slot

import play.api.libs.json.{ Json, Reads }

case class CreateSlotRequest(slotType: String, dateTime: Long)

object CreateSlotRequest {
  implicit val createSlotRequestRead: Reads[CreateSlotRequest] = Json.reads[CreateSlotRequest]
}
