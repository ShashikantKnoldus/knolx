package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Session
import play.api.libs.json.{ Json, OWrites }

final case class GetTopicsBySubCategoryResponse(topics: Seq[String])

object GetTopicsBySubCategoryResponse {

  implicit val GetTopicsBySubCategoryResponseWrites: OWrites[GetTopicsBySubCategoryResponse] =
    Json.writes[GetTopicsBySubCategoryResponse]

  def fromDomain(category: Seq[WithId[Session]]): GetTopicsBySubCategoryResponse = {
    val categories = category.map { sessionInfo =>
      sessionInfo.entity.topic
    }
    GetTopicsBySubCategoryResponse(categories)
  }
}
