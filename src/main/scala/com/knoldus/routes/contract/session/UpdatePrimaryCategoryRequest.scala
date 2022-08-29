package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

final case class UpdatePrimaryCategoryRequest(newCategoryName: String)

object UpdatePrimaryCategoryRequest {

  implicit val UpdatePrimaryCategoryRequestReads: Reads[UpdatePrimaryCategoryRequest] =
    Json.reads[UpdatePrimaryCategoryRequest]
}
