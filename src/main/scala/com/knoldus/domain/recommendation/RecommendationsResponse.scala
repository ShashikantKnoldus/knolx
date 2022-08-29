package com.knoldus.domain.recommendation

final case class RecommendationsResponse(
  email: String,
  recommendationId: String,
  upVote: Boolean,
  downVote: Boolean
)
