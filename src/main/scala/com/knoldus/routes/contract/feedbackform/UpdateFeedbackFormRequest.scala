package com.knoldus.routes.contract.feedbackform

import com.knoldus.domain.feedbackform.Question
import play.api.libs.json.{ Json, Reads }

final case class UpdateFeedbackFormRequest(
  name: Option[String] = None,
  questions: Option[List[Question]] = None
)

object UpdateFeedbackFormRequest {
  implicit val UpdateFeedbackFormReads: Reads[UpdateFeedbackFormRequest] = Json.reads[UpdateFeedbackFormRequest]
}
