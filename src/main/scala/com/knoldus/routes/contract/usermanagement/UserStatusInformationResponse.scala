package com.knoldus.routes.contract.usermanagement

import play.api.libs.json.{ Json, OWrites }

final case class UserStatusInformationResponse(
  email: String,
  active: Boolean,
  ban: Boolean,
  coreMember: Boolean,
  admin: Boolean,
  password: Option[String]
)

object UserStatusInformationResponse {

  implicit val UserStatusInformationWrites: OWrites[UserStatusInformationResponse] =
    Json.writes[UserStatusInformationResponse]
}
