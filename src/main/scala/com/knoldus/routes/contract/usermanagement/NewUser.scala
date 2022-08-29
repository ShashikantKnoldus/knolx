package com.knoldus.routes.contract.usermanagement

import play.api.libs.json.{ Json, OWrites }

final case class NewUser(name: String, email: String)

object NewUser {
  implicit val NewUsersWrites: OWrites[NewUser] = Json.writes[NewUser]
}
