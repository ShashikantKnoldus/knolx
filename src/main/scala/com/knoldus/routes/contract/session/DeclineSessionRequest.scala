package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

final case class DeclineSessionRequest(approved: Boolean, declined: Boolean)

object DeclineSessionRequest {
  implicit val DeclineSessionRequestReads: Reads[DeclineSessionRequest] = Json.reads[DeclineSessionRequest]
}
