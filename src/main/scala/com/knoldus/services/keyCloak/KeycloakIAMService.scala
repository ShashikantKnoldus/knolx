package com.knoldus.services.keyCloak

import akka.event.LoggingAdapter
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.usermanagement.NewUser
import com.knoldus.services.IAM.IAMService
import com.typesafe.config.Config
import org.keycloak.adapters.KeycloakDeploymentBuilder
import org.keycloak.adapters.rotation.AdapterTokenVerifier

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }
import scala.jdk.CollectionConverters.CollectionHasAsScala

class KeycloakIAMService(val keycloakInstance: KeycloakInstance, val conf: Config)(implicit
  val logger: LoggingAdapter,
  val ex: ExecutionContext
) extends IAMService {

  override def validateTokenAndGetDetails(token: String): Future[Option[NewUserInformation]] = {
    val keycloakConfig = getClass.getResourceAsStream("/keycloak.json")
    val keycloakBuild = KeycloakDeploymentBuilder.build(keycloakConfig)
    val futureAccessTokenResponse = Future(Try(AdapterTokenVerifier.verifyToken(token, keycloakBuild)))
    futureAccessTokenResponse.map {
      case Success(accessToken) =>
        val email = accessToken.getEmail
        val name = accessToken.getGivenName.trim + " " + accessToken.getFamilyName.trim
        val resourceName = "leaderboard-ui"
        val roles = accessToken.getResourceAccess(resourceName).getRoles
        if (roles.contains(Admin.toString))
          Some(NewUserInformation(email, name, Admin))
        else
          Some(NewUserInformation(email, name, Employee))

      case Failure(exception) =>
        logger.info(s"Token Invalid $exception")
        None
    }
  }

  override def getUsers: Future[List[NewUser]] = {
    val keycloak = keycloakInstance.getKeycloakInstance(conf)
    val usersList = Future(keycloak.realm("knoldus").users().list().asScala.toList)
    usersList.map { users =>
      users.map { user =>
        NewUser(
          name =
            if (user.getLastName == "")
              user.getFirstName
            else
              user.getFirstName + " " + user.getLastName,
          email = user.getEmail
        )

      }
    }
  }
}
