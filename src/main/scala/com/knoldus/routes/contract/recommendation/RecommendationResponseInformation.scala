package com.knoldus.routes.contract.recommendation

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.recommendation.RecommendationsResponse
import play.api.libs.json.{ Json, OWrites }

final case class RecommendationResponseInformation(
  email: String,
  recommendationId: String,
  upVote: Boolean,
  downVote: Boolean,
  id: String
)

object RecommendationResponseInformation {

  implicit val RecommendationResponseInformationWrites: OWrites[RecommendationResponseInformation] =
    Json.writes[RecommendationResponseInformation]

  def fromDomain(recommendation: WithId[RecommendationsResponse]): RecommendationResponseInformation =
    recommendation match {
      case WithId(entity, id) =>
        RecommendationResponseInformation(
          entity.email,
          entity.recommendationId,
          entity.upVote,
          entity.downVote,
          id
        )
    }
}
