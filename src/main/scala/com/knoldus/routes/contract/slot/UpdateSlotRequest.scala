package com.knoldus.routes.contract.slot

import play.api.libs.json.{ Json, Reads }

final case class UpdateSlotRequest(
  newSlotType: String,
  newDateTime: Long
)

object UpdateSlotRequest {

  implicit val updateSlotRequestReads: Reads[UpdateSlotRequest] =
    Json.reads[UpdateSlotRequest]
}
