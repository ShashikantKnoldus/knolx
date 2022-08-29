package com.knoldus.routes.contract.department

import play.api.libs.json.{ Json, Reads }

final case class UpdateDepartmentRequest(
  newDepartmentName: Option[String],
  newQuota: Option[Int],
  newEmail: Option[String]
)

object UpdateDepartmentRequest {

  implicit val updateDepartmentRequestReads: Reads[UpdateDepartmentRequest] =
    Json.reads[UpdateDepartmentRequest]
}
