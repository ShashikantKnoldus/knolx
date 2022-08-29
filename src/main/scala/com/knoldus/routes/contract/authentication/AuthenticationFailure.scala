package com.knoldus.routes.contract.authentication

import play.api.libs.json.{ Json, OWrites }

final case class AuthenticationFailure(status: Boolean, errorMessage: String)

object AuthenticationFailure {
  implicit val AuthenticationFailureWrites: OWrites[AuthenticationFailure] = Json.writes[AuthenticationFailure]
}
