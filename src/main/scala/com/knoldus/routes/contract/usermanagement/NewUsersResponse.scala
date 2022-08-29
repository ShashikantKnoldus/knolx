package com.knoldus.routes.contract.usermanagement

import play.api.libs.json.{ Json, OWrites }

case class NewUsersResponse(users: List[NewUser])

object NewUsersResponse {

  implicit val UsersResponseWrites: OWrites[NewUsersResponse] = Json.writes[NewUsersResponse]

  def fromDomain(users: List[NewUser]): NewUsersResponse =
    NewUsersResponse(
      users
    )
}
