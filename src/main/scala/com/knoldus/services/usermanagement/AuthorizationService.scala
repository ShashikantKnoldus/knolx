package com.knoldus.services.usermanagement

import akka.event.LoggingAdapter
import akka.http.caching.scaladsl.Cache
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.user.{ NewUserInformation, UserToken }
import com.knoldus.services.IAM.IAMService
import com.knoldus.services.common.DateTimeUtility

import scala.concurrent.{ ExecutionContext, Future }

class AuthorizationService(
  cache: Cache[String, UserToken],
  clientSecretCache: Cache[String, String],
  userDao: UserDao,
  iamService: IAMService
)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {

  val dateTimeUtility = new DateTimeUtility

  def validateClientSecrets(clientId: String, clientSecret: String): Future[Boolean] =
    clientSecretCache.get(clientId) match {
      case Some(cachedClientSecret) => cachedClientSecret.map(_ == clientSecret)
      case None => Future.successful(false)
    }

  def validateBearerToken(tokenToVerify: String): Future[Option[NewUserInformation]] = {
    val tokenValidation = iamService.validateTokenAndGetDetails(tokenToVerify)
    tokenValidation.map {
      case Some(userDetails) => Some(NewUserInformation(userDetails.email, userDetails.name, userDetails.keycloakRole))
      case _ => None
    }
  }
}

object AuthorizationService {
  sealed trait AuthorizationServiceError
  case object InvalidToken extends AuthorizationServiceError
}
