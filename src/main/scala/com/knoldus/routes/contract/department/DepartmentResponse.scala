package com.knoldus.routes.contract.department

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.department.Department
import play.api.libs.json.{ Json, OWrites }

final case class DepartmentResponse(
  id: String,
  name: String,
  email: Option[String],
  quota: Int,
  createdon: Long,
  lastUpdatedon: Long
)

object DepartmentResponse {
  implicit val CreateDepartmentResponseWrites: OWrites[DepartmentResponse] = Json.writes[DepartmentResponse]

  def fromDomain(department: WithId[Department]): DepartmentResponse =
    department match {
      case WithId(entity, id) =>
        DepartmentResponse(
          id,
          entity.name,
          entity.headEmail,
          entity.quota,
          entity.createdOn,
          entity.lastUpdated
        )
    }
}
