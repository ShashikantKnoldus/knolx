package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites }

final case class UpdateSuccessResponse(status: String)

object UpdateSuccessResponse {
  implicit val SuccessResponseWrites: OWrites[UpdateSuccessResponse] = Json.writes[UpdateSuccessResponse]
}
