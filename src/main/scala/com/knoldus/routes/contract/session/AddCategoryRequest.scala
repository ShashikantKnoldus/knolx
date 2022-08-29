package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

final case class AddCategoryRequest(categoryName: String, subCategory: Seq[String] = Seq.empty)

object AddCategoryRequest {
  implicit val AddCategoryRequestReads: Reads[AddCategoryRequest] = Json.reads[AddCategoryRequest]
}
