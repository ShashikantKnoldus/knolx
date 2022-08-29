package com.knoldus.routes.contract.feedbackform

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.feedbackform.{ FeedbackForm, Question }
import play.api.libs.json.{ Json, OWrites }

final case class FeedbackFormResponse(
  name: String,
  questions: List[Question],
  active: Boolean = true
)

object FeedbackFormResponse {

  implicit val CreateFeedbackFormResponseWrites: OWrites[FeedbackFormResponse] =
    Json.writes[FeedbackFormResponse]

  def fromDomain(feedbackForm: WithId[FeedbackForm]): FeedbackFormResponse =
    feedbackForm match {
      case WithId(entity, _) =>
        FeedbackFormResponse(
          entity.name,
          entity.questions,
          entity.active
        )
    }

}
