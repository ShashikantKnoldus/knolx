package com.knoldus.domain.feedbackform

import java.util.Date

import com.knoldus.routes.contract.feedbackform.QuestionResponse

final case class UserFeedbackResponse(
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
  sessionDate: Date
)
