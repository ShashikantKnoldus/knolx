package com.knoldus.domain.session

import java.util.Date

final case class SessionRequest(
  approved: Boolean,
  category: String,
  date: Date,
  decline: Boolean,
  email: String,
  freeSlot: Boolean,
  meetup: Boolean,
  notification: Boolean,
  recommendationId: String,
  subCategory: String,
  topic: String,
  brief: String
)
