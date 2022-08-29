package com.knoldus.routes.contract.usermanagement

import play.api.libs.json.{ Json, Reads }

final case class UpdateUserStatusRequest(
  email: String,
  active: Boolean,
  ban: Boolean,
  coreMember: Boolean,
  admin: Boolean,
  password: Option[String]
)

object UpdateUserStatusRequest {
  implicit val updateUserRequestReads: Reads[UpdateUserStatusRequest] = Json.reads[UpdateUserStatusRequest]
}
