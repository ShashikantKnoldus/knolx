package com.knoldus.domain.recommendation

import java.util.Date

final case class Recommendation(
  email: Option[String],
  name: String,
  topic: String,
  description: String,
  submissionDate: Date,
  updateDate: Date,
  approved: Boolean,
  decline: Boolean,
  pending: Boolean,
  done: Boolean,
  book: Boolean,
  upVote: Int,
  downVote: Int
)
