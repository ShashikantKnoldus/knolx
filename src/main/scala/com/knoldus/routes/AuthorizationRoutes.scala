package com.knoldus.routes

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Directive0, Directive1 }
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.authentication.AuthenticationFailure
import com.knoldus.services.usermanagement.AuthorizationService
import play.api.libs.json.Json

trait AuthorizationRoutes extends BaseRoutes {

  val authorizationService: AuthorizationService

  def clientAuthorizationWithHeader: Directive0 =
    optionalHeaderValueByName("clientId").flatMap {
      case Some(clientID) =>
        optionalHeaderValueByName("clientSecret").flatMap {
          case Some(clientSecret) => authenticateClientCredentials(clientID, clientSecret)
          case None => complete(respondWithFailure("clientSecret not provided"))
        }
      case None => complete(respondWithFailure("clientId not provided"))
    }

  def authenticationWithBearerToken: Directive1[NewUserInformation] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(token) =>
        val bearerToken = token.split(" ")(1)
        authenticateBearerToken(bearerToken)
      case None =>
        complete(respondWithFailure("Authorization Token not provided"))
    }

  private def authenticateBearerToken(token: String): Directive1[NewUserInformation] =
    for {
      response <- onSuccess(authorizationService.validateBearerToken(token))
      result <- handleServiceResult(response)
    } yield result

  private def handleServiceResult(
    result: Option[NewUserInformation]
  ): Directive1[NewUserInformation] =
    result match {
      case Some(userInformation) => provide(userInformation)
      case None => complete(respondWithFailure("Authorization failure"))
    }

  private def authenticateClientCredentials(
    clientId: String,
    clientSecret: String
  ): Directive0 =
    onSuccess(authorizationService.validateClientSecrets(clientId, clientSecret)).flatMap { isValidated =>
      if (isValidated) pass
      else complete(respondWithFailure("Authorization failure"))
    }

  private def respondWithFailure(errorMessage: String) = {
    val jsonResponse = Json.toJson(AuthenticationFailure(false, errorMessage)).toString()
    HttpResponse(status = StatusCodes.Unauthorized, entity = HttpEntity(ContentTypes.`application/json`, jsonResponse))
  }
}
