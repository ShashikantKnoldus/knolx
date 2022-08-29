package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

final case class AddSubCategoryRequest(categoryId: String, subCategory: String)

object AddSubCategoryRequest {
  implicit val AddSubCategoryRequestReads: Reads[AddSubCategoryRequest] = Json.reads[AddSubCategoryRequest]
}
