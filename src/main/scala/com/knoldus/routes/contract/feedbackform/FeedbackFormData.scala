package com.knoldus.routes.contract.feedbackform

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.feedbackform.Question
import play.api.libs.json.{ Json, OWrites }

final case class FeedbackFormData(name: String, questions: List[Question], active: Boolean, id: String)

object FeedbackFormData {

  implicit val CreateFeedbackFormResponseWrites: OWrites[FeedbackFormData] =
    Json.writes[FeedbackFormData]

  def fromDomain(feedbackForm: WithId[FeedbackFormData]): FeedbackFormData =
    feedbackForm match {
      case WithId(entity, id) =>
        FeedbackFormData(
          entity.name,
          entity.questions,
          entity.active,
          id
        )
    }

}
