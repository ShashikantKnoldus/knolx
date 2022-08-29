package com.knoldus.routes.contract.feedbackform

import java.util.Date

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.feedbackform.UserFeedbackResponse
import play.api.libs.json.{ Json, OWrites }

final case class FeedbackFormFetchResponse(
  email: String,
  coreMember: Boolean,
  presenter: String,
  userId: String,
  sessionId: String,
  sessionTopic: String,
  meetup: Boolean,
  sessionDate: Date,
  session: String,
  feedbackResponse: List[QuestionResponse],
  responseDate: Date,
  score: Double
)

object FeedbackFormFetchResponse {

  implicit val FeedbackFetchResponseWrites: OWrites[FeedbackFormFetchResponse] =
    Json.writes[FeedbackFormFetchResponse]

  def fromDomain(feedbackResponse: WithId[UserFeedbackResponse]): FeedbackFormFetchResponse =
    FeedbackFormFetchResponse(
      feedbackResponse.entity.email,
      feedbackResponse.entity.coreMember,
      feedbackResponse.entity.presenter,
      feedbackResponse.entity.userId,
      feedbackResponse.entity.sessionId,
      feedbackResponse.entity.sessionTopic,
      feedbackResponse.entity.meetup,
      feedbackResponse.entity.sessionDate,
      feedbackResponse.entity.session,
      feedbackResponse.entity.feedbackResponse,
      feedbackResponse.entity.responseDate,
      feedbackResponse.entity.score
    )

}
