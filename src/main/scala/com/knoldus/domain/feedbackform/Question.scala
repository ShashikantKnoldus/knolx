package com.knoldus.domain.feedbackform

import play.api.libs.json.{ Json, OWrites, Reads }

final case class Question(
  question: String,
  options: List[String],
  questionType: String,
  mandatory: Boolean
)

object Question {
  implicit val QuestionInformationReads: Reads[Question] = Json.reads[Question]
  implicit val QuestionInformationWrites: OWrites[Question] = Json.writes[Question]
}
