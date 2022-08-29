package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites }

final case class CategoryInformation(categoryId: String, categoryName: String, subCategory: Seq[String])

object CategoryInformation {
  implicit val CategoryInformationWrites: OWrites[CategoryInformation] = Json.writes[CategoryInformation]
}
