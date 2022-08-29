package com.knoldus.routes.contract.slot

import play.api.libs.json.{ Json, OWrites }

final case class UpdateSlotResponse(status: Boolean)

object UpdateSlotResponse {
  implicit val updateSlotResponseResponse: OWrites[UpdateSlotResponse] = Json.writes[UpdateSlotResponse]
}
