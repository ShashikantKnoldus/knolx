package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

final case class EmailRequest(sessionId: String)

object EmailRequest {
  implicit val EmailRequestWrites: Reads[EmailRequest] = Json.reads[EmailRequest]
}
