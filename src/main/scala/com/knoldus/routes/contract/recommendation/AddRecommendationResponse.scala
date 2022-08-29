package com.knoldus.routes.contract.recommendation

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.recommendation.Recommendation
import java.util.Date
import play.api.libs.json.{ Json, OWrites }

final case class AddRecommendationResponse(
  email: Option[String],
  name: String,
  topic: String,
  description: String,
  submissionDate: Date,
  updateDate: Date,
  approved: Boolean,
  declined: Boolean,
  pending: Boolean,
  done: Boolean,
  book: Boolean,
  upVotes: Int,
  downVotes: Int,
  _id: String
)

object AddRecommendationResponse {
  implicit val CreateSessionResponseWrites: OWrites[AddRecommendationResponse] = Json.writes[AddRecommendationResponse]

  def fromDomain(recommendation: WithId[Recommendation]): AddRecommendationResponse =
    recommendation match {
      case WithId(entity, id) =>
        AddRecommendationResponse(
          entity.email,
          entity.name,
          entity.topic,
          entity.description,
          entity.submissionDate,
          entity.updateDate,
          entity.approved,
          entity.decline,
          entity.pending,
          entity.done,
          entity.book,
          entity.upVote,
          entity.downVote,
          id
        )
    }
}
