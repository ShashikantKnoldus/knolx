package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Category
import play.api.libs.json.{ Json, OWrites }

final case class AddCategoryResponse(categoryId: String, categoryName: String, subCategory: Seq[String])

object AddCategoryResponse {
  implicit val AddCategoryResponseWrites: OWrites[AddCategoryResponse] = Json.writes[AddCategoryResponse]

  def fromDomain(category: WithId[Category]): AddCategoryResponse =
    category match {
      case WithId(entity, id) =>
        AddCategoryResponse(id, entity.categoryName, entity.subCategory)
    }
}
