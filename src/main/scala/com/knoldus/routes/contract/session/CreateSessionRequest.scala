package com.knoldus.routes.contract.session

import java.util.Date
import play.api.libs.json.{ Json, Reads }

final case class CreateSessionRequest(
  email: String,
  date: Date,
  session: String,
  category: String,
  subCategory: String,
  feedbackFormId: String,
  topic: String,
  brief: String,
  feedbackExpirationDays: Int,
  meetup: Boolean
)

object CreateSessionRequest {
  implicit val CreateSessionRequestReads: Reads[CreateSessionRequest] = Json.reads[CreateSessionRequest]
}
