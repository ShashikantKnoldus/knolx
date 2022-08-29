package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

final case class DeleteSubCategoryRequest(subCategory: String)

object DeleteSubCategoryRequest {
  implicit val DeleteSubCategoryRequestReads: Reads[DeleteSubCategoryRequest] = Json.reads[DeleteSubCategoryRequest]
}
