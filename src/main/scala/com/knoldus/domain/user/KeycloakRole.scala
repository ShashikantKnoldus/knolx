package com.knoldus.domain.user

object KeycloakRole extends Enumeration {
  type KeycloakRole = Value

  val Employee: KeycloakRole.Value = Value("employee")
  val Admin: KeycloakRole.Value = Value("admin")
}
