package com.knoldus.routes.contract.usermanagement

import java.util.Date
import play.api.libs.json.{ Json, OWrites }

final case class UserInformationResponse(
  email: String,
  active: Boolean,
  admin: Boolean,
  coreMember: Boolean,
  superUser: Boolean,
  banTill: Date,
  banCount: Int = 0,
  id: String,
  department: Option[String]
)

object UserInformationResponse {
  implicit val userResponseWrites: OWrites[UserInformationResponse] = Json.writes[UserInformationResponse]
}
