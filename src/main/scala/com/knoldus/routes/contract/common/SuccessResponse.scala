package com.knoldus.routes.contract.common

import play.api.libs.json.{ Json, OWrites }

final case class SuccessResponse(status: Boolean)

object SuccessResponse {
  implicit val SuccessResponseWrites: OWrites[SuccessResponse] = Json.writes[SuccessResponse]
}
