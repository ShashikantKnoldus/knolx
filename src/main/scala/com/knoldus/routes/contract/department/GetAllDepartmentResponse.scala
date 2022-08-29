package com.knoldus.routes.contract.department

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.department.Department
import play.api.libs.json.{ Json, OWrites }

final case class GetAllDepartmentResponse(departments: Seq[DepartmentResponse])

object GetAllDepartmentResponse {
  implicit val GetAllDepartmentResponseWrites: OWrites[GetAllDepartmentResponse] = Json.writes[GetAllDepartmentResponse]

  def fromDomain(departments: Seq[WithId[Department]]): GetAllDepartmentResponse = {
    val department = departments.map(departmentInfo =>
      DepartmentResponse(
        departmentInfo.id,
        departmentInfo.entity.name,
        departmentInfo.entity.headEmail,
        departmentInfo.entity.quota,
        departmentInfo.entity.createdOn,
        departmentInfo.entity.lastUpdated
      )
    )
    GetAllDepartmentResponse(department)
  }
}
