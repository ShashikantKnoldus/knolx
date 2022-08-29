package com.knoldus.routes.contract.usermanagement

import play.api.libs.json.{ Json, Reads }

final case class UserDepartmentUpdateRequest(
  newDepartment: String
)

object UserDepartmentUpdateRequest {
  implicit val updateUserRequestReads: Reads[UserDepartmentUpdateRequest] = Json.reads[UserDepartmentUpdateRequest]
}
