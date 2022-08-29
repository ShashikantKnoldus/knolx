package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

final case class UpdateSubcategoryRequest(oldSubCategory: String, newSubCategory: String)

object UpdateSubcategoryRequest {
  implicit val UpdateSubcategoryRequestReads: Reads[UpdateSubcategoryRequest] = Json.reads[UpdateSubcategoryRequest]
}
