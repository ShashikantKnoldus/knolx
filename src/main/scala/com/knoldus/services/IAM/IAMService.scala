package com.knoldus.services.IAM

import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.usermanagement.NewUser

import scala.concurrent.Future

trait IAMService {
  def validateTokenAndGetDetails(tokenToVerify: String): Future[Option[NewUserInformation]]
  def getUsers: Future[List[NewUser]]
}
