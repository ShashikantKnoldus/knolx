package com.knoldus.domain.feedbackform

import com.knoldus.dao.commons.WithId
import play.api.libs.json.{ Json, OWrites }

final case class FeedbackFormList(name: String, questions: List[Question], active: Boolean, id: String)

object FeedbackFormList {

  def fromDomain(feedbackForm: WithId[FeedbackFormList]): FeedbackFormList =
    feedbackForm match {
      case WithId(entity, id) =>
        FeedbackFormList(
          entity.name,
          entity.questions,
          entity.active,
          id
        )
    }

  implicit val CreateFeedbackFormResponseWrites: OWrites[FeedbackFormList] =
    Json.writes[FeedbackFormList]
}
