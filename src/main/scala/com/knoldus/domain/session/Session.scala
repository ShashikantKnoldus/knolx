package com.knoldus.domain.session

import java.util.Date

final case class Session(
  userId: String,
  email: String,
  date: Date,
  session: String,
  category: String,
  subCategory: String,
  feedbackFormId: String,
  topic: String,
  feedbackExpirationDays: Int,
  meetup: Boolean,
  brief: String,
  rating: String,
  score: Double,
  cancelled: Boolean,
  active: Boolean,
  expirationDate: Date,
  youtubeURL: Option[String],
  slideShareURL: Option[String],
  temporaryYoutubeURL: Option[String],
  reminder: Boolean,
  notification: Boolean
)
