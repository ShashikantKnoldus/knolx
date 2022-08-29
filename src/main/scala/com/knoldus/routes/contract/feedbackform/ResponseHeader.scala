package com.knoldus.routes.contract.feedbackform

import java.util.Date

import play.api.libs.json.{ Json, OWrites, Reads }

final case class ResponseHeader(topic: String, email: String, date: Date, session: String, meetUp: Boolean)

object ResponseHeader {
  implicit val ResponseHeaderWrites: OWrites[ResponseHeader] = Json.writes[ResponseHeader]
  implicit val ResponseHeaderReads: Reads[ResponseHeader] = Json.reads[ResponseHeader]
}
