package com.knoldus.routes.contract.department

import play.api.libs.json.{ Json, Reads }

final case class DepartmentRequest(
  name: String,
  headEmail: Option[String],
  quota: Int
)

object DepartmentRequest {
  implicit val AddDepartmentRequestReads: Reads[DepartmentRequest] = Json.reads[DepartmentRequest]
}
