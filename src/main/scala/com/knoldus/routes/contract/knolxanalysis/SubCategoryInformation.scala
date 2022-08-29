package com.knoldus.routes.contract.knolxanalysis

import play.api.libs.json.{ Json, OWrites }

final case class SubCategoryInformation(subCategoryName: String, totalSessionSubCategory: Int)

object SubCategoryInformation {
  implicit val SubCategoryInformationWrites: OWrites[SubCategoryInformation] = Json.writes[SubCategoryInformation]
}
