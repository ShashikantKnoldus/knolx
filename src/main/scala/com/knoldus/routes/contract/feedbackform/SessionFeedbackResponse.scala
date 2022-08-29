package com.knoldus.routes.contract.feedbackform

import java.util.Date

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.feedbackform.UserFeedbackResponse
import play.api.libs.json.{ Json, OWrites, Reads }

final case class SessionFeedbackResponse(
  sessionId: String,
  userId: String,
  coreMember: Boolean,
  email: String,
  feedbackResponse: List[QuestionResponse],
  meetup: Boolean,
  presenter: String,
  responseDate: Date,
  score: Double,
  session: String,
  sessionTopic: String,
  sessiondate: Date
)

object SessionFeedbackResponse {
  implicit val StoreFeedbackWrites: OWrites[SessionFeedbackResponse] = Json.writes[SessionFeedbackResponse]
  implicit val StoreFeedbackReads: Reads[SessionFeedbackResponse] = Json.reads[SessionFeedbackResponse]

  def fromDomain(userResponse: WithId[UserFeedbackResponse]): SessionFeedbackResponse =
    userResponse match {
      case WithId(entity, sessionId) =>
        SessionFeedbackResponse(
          sessionId,
          entity.userId,
          entity.coreMember,
          entity.email,
          entity.feedbackResponse,
          entity.meetup,
          entity.presenter,
          entity.responseDate,
          entity.score,
          entity.session,
          entity.sessionTopic,
          entity.sessionDate
        )
    }

}
