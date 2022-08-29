package com.knoldus.routes.contract.feedbackform

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.feedbackform.FeedbackForm
import play.api.libs.json.{ Json, Writes }

final case class FeedbackFormsToday(feedbackSession: List[(FeedbackSession, String)])

object FeedbackFormsToday {

  implicit val FeedbackFormsTodayWrites: Writes[FeedbackFormsToday] = Json.writes[FeedbackFormsToday]

  def fromdomain(
    feedbackSessionWithForm: List[(FeedbackSession, WithId[FeedbackForm])]
  ): List[(FeedbackSession, String)] = {

    val feedbackList = feedbackSessionWithForm.map { tuple =>
      val (_, formData) = tuple

      val feedbackForm = formData
      val form = FeedbackFormData(
        feedbackForm.entity.name,
        feedbackForm.entity.questions,
        feedbackForm.entity.active,
        feedbackForm.id
      )
      Json.toJson(form).toString()
    }

    val feedbackSessions = feedbackSessionWithForm.map { tuple =>
      val (feedbackWithSession, _) = tuple
      feedbackWithSession
    }

    feedbackSessions zip feedbackList
  }

}
