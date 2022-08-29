package com.knoldus.routes.contract.recommendation

import play.api.libs.json.{ Json, Reads }

final case class RecommendationInformation(email: Option[String], name: String, topic: String, description: String)

object RecommendationInformation {
  implicit val RecommendationInformationReads: Reads[RecommendationInformation] = Json.reads[RecommendationInformation]
}
