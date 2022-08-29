package com.knoldus.routes.contract.feedbackform

import play.api.libs.json.{ Json, OWrites, Reads }

final case class QuestionResponse(question: String, options: List[String], response: String)

object QuestionResponse {
  implicit val QuestionResponseWrites: OWrites[QuestionResponse] = Json.writes[QuestionResponse]
  implicit val QuestionResponseReads: Reads[QuestionResponse] = Json.reads[QuestionResponse]

}
