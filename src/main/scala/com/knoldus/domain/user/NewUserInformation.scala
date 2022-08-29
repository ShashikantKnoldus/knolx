package com.knoldus.domain.user

import com.knoldus.domain.user.KeycloakRole._

case class NewUserInformation(email: String, name: String, keycloakRole: KeycloakRole) {

  def isAdmin: Boolean =
    if (keycloakRole == Admin)
      true
    else
      false
}
