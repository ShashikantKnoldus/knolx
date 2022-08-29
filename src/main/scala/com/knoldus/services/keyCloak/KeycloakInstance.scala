package com.knoldus.services.keyCloak

import com.typesafe.config.Config
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.keycloak.admin.client.{ Keycloak, KeycloakBuilder }

class KeycloakInstance(conf: Config) {

  def getKeycloakInstance(conf: Config): Keycloak = {
    val keycloakConf = conf.getConfig("keycloak")
    val serverUrl = keycloakConf.getString("realmServerURL")
    val realm = keycloakConf.getString("realmName")
    val userName = keycloakConf.getString("username")
    val password = keycloakConf.getString("password")
    val clientId = keycloakConf.getString("clientId")
    val clientSecret = keycloakConf.getString("clientSecret")
    val keycloakBuild = KeycloakBuilder
      .builder()
      .serverUrl(serverUrl)
      .realm(realm)
      .username(userName)
      .password(password)
      .clientId(clientId)
      .clientSecret(clientSecret)
      .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(20).build())
      .build()
    keycloakBuild
  }
}
