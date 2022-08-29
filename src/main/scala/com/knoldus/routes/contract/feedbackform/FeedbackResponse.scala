package com.knoldus.routes.contract.feedbackform

import play.api.libs.json.{ Json, Reads }

final case class FeedbackResponse(sessionId: String, feedbackFormId: String, responses: List[String], score: Double)

object FeedbackResponse {
  implicit val FeedbackResponseReads: Reads[FeedbackResponse] = Json.reads[FeedbackResponse]
}
