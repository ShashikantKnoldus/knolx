package com.knoldus.routes.contract.department

import play.api.libs.json.{ Json, OWrites }

final case class UpdateDepartmentResponse(status: Boolean)

object UpdateDepartmentResponse {
  implicit val updateDepartmentResponse: OWrites[UpdateDepartmentResponse] = Json.writes[UpdateDepartmentResponse]
}
