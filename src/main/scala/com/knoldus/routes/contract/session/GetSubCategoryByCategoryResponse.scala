package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Category
import play.api.libs.json.{ Json, OWrites }

final case class GetSubCategoryByCategoryResponse(subCategories: Seq[String])

object GetSubCategoryByCategoryResponse {

  implicit val GetSubCategoryByCategoryResponseWrites: OWrites[GetSubCategoryByCategoryResponse] =
    Json.writes[GetSubCategoryByCategoryResponse]

  def fromDomain(category: WithId[Category]): GetSubCategoryByCategoryResponse =
    category match {
      case WithId(entity, _) =>
        GetSubCategoryByCategoryResponse(entity.subCategory)
    }
}
