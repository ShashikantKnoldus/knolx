package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Category
import play.api.libs.json.{ Json, OWrites }

final case class GetAllCategoryResponse(categories: Seq[CategoryInformation])

object GetAllCategoryResponse {
  implicit val GetAllCategoryResponseWrites: OWrites[GetAllCategoryResponse] = Json.writes[GetAllCategoryResponse]

  def fromDomain(category: Seq[WithId[Category]]): GetAllCategoryResponse = {
    val primaryCategory = category.map(categoryInfo =>
      CategoryInformation(categoryInfo.id, categoryInfo.entity.categoryName, categoryInfo.entity.subCategory)
    )
    GetAllCategoryResponse(primaryCategory)
  }
}
