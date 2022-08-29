package com.knoldus.routes.contract.feedbackform

import java.util.Date

import play.api.libs.json.{ Json, OWrites }

final case class FeedbackSession(
  userId: String,
  email: String,
  date: Date,
  session: String,
  feedbackFormId: String,
  topic: String,
  meetup: Boolean,
  rating: String,
  cancelled: Boolean,
  active: Boolean,
  id: String,
  expirationDate: Date,
  feedbackSubmitted: Boolean = false
)

object FeedbackSession {
  implicit val FeedbackSessionsWrites: OWrites[FeedbackSession] = Json.writes[FeedbackSession]

}
