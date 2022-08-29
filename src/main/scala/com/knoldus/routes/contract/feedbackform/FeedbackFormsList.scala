package com.knoldus.routes.contract.feedbackform

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.feedbackform.{ FeedbackForm, Question }
import play.api.libs.json.{ Json, OWrites }

final case class FeedbackFormsList(updateInfo: List[UpdateFeedbackFormInformation], count: Int, pages: Int)

object FeedbackFormsList {
  implicit val FeedbackFormResponseAllWrites: OWrites[FeedbackFormsList] = Json.writes[FeedbackFormsList]

  def fromDomain(
    feedbackFormEntity: Seq[WithId[FeedbackForm]],
    pageNumber: Int,
    count: Int
  ): FeedbackFormsList = {
    val updateFormInformation = feedbackFormEntity.map { feedbackForm =>
      val questionInformation = feedbackForm.entity.questions.map(question =>
        Question(
          question.question,
          question.options,
          question.questionType,
          question.mandatory
        )
      )

      UpdateFeedbackFormInformation(
        feedbackForm.id,
        feedbackForm.entity.name,
        questionInformation
      )
    }
    FeedbackFormsList(updateFormInformation.toList, pageNumber, count)
  }
}
