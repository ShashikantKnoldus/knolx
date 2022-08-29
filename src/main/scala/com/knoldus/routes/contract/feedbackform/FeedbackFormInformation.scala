package com.knoldus.routes.contract.feedbackform

import com.knoldus.domain.feedbackform.Question
import play.api.libs.json.{ Json, Reads }

final case class FeedbackFormInformation(name: String, questions: List[Question])

object FeedbackFormInformation {
  implicit val CreateFeedbackFormReads: Reads[FeedbackFormInformation] = Json.reads[FeedbackFormInformation]
}
