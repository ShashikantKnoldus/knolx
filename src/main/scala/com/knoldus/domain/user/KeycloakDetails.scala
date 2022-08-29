package com.knoldus.domain.user

import com.knoldus.domain.user.KeycloakRole.KeycloakRole

case class KeycloakDetails(email: String, keycloakRole: KeycloakRole)
