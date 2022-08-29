package com.knoldus.routes.contract.usermanagement

import play.api.libs.json.{ Json, OWrites }

final case class SearchUserResponse(
  email: String,
  active: Boolean,
  id: String,
  banTill: String,
  admin: Boolean = false,
  superUser: Boolean = false,
  coreMember: Boolean = false,
  ban: Boolean = false,
  department: Option[String]
)

object SearchUserResponse {
  implicit val searchUserResponseWrites: OWrites[SearchUserResponse] = Json.writes[SearchUserResponse]
}
