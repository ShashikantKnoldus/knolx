package com.knoldus.routes.contract.knolxanalysis

import play.api.libs.json.{ Json, OWrites }

final case class CategoryInformation(
  categoryName: String,
  totalSessionCategory: Int,
  subCategoryInfo: List[SubCategoryInformation]
)

object CategoryInformation {
  implicit val categoryInformationWrites: OWrites[CategoryInformation] = Json.writes[CategoryInformation]
}
