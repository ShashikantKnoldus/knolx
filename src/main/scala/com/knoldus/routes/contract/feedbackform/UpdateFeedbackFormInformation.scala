package com.knoldus.routes.contract.feedbackform

import com.knoldus.domain.feedbackform.Question
import play.api.libs.json.{ Json, OWrites }

final case class UpdateFeedbackFormInformation(id: String, name: String, questions: List[Question])

object UpdateFeedbackFormInformation {
  implicit val UpdateInfoWrites: OWrites[UpdateFeedbackFormInformation] = Json.writes[UpdateFeedbackFormInformation]

}
