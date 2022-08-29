package com.knoldus.domain.feedbackform

import java.util.Date

import com.knoldus.routes.contract.feedbackform.QuestionResponse

final case class UserSessionFeedbackResponse(
  email: String,
  coreMember: Boolean,
  presenter: String,
  userId: String,
  sessionId: String,
  sessionTopic: String,
  meetup: Boolean,
  sessiondate: Date,
  session: String,
  feedbackResponse: List[QuestionResponse],
  responseDate: Date,
  score: Double
)
