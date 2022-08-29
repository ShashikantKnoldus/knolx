package com.knoldus.routes.contract.feedbackform

final case class FeedbackReportHeader(
  sessionId: String,
  topic: String,
  presenter: String,
  active: Boolean,
  session: String,
  meetUp: Boolean,
  date: String,
  rating: String,
  expired: Boolean
)
