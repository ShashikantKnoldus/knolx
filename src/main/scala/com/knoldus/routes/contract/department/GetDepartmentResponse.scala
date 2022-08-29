package com.knoldus.routes.contract.department

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.department.Department
import play.api.libs.json.{ Json, OWrites }

final case class GetDepartmentResponse(
  departmentId: String,
  departmentName: String,
  headEmail: Option[String],
  quota: Int,
  createdOn: Long,
  lastUpdated: Long
)

object GetDepartmentResponse {
  implicit val GetAllDepartmentResponseWrites: OWrites[GetDepartmentResponse] = Json.writes[GetDepartmentResponse]

  def fromDomain(departments: WithId[Department]): GetDepartmentResponse = {
    val department = GetDepartmentResponse(
      departments.id,
      departments.entity.name,
      departments.entity.headEmail,
      departments.entity.quota,
      departments.entity.createdOn,
      departments.entity.lastUpdated
    )
    department
  }
}
