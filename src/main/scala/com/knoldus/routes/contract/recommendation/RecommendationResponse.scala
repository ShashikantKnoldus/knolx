package com.knoldus.routes.contract.recommendation

import com.knoldus.domain.recommendation.RecommendationsResponse
import play.api.libs.json.{ Json, Reads, Writes }

final case class RecommendationResponse(
  email: String,
  recommendationId: String,
  upVote: Boolean,
  downVote: Boolean
)

object RecommendationResponse {
  implicit val RecommendationResponse: Reads[RecommendationResponse] = Json.reads[RecommendationResponse]
  implicit val RecommendationResponseWrites: Writes[RecommendationsResponse] = Json.writes[RecommendationsResponse]

}
