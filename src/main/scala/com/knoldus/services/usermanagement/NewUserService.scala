package com.knoldus.services.usermanagement

import akka.event.LoggingAdapter
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.CheckActiveUsers
import com.knoldus.routes.contract.usermanagement.NewUser
import com.knoldus.services.IAM.IAMService
import com.typesafe.config.Config

import scala.concurrent.{ ExecutionContext, Future }

class NewUserService(userDao: UserDao, config: Config, iamService: IAMService)(implicit
  val ec: ExecutionContext,
  logger: LoggingAdapter
) {

  def getAllUsers: Future[List[NewUser]] =
    iamService.getUsers

  def getUsers: Future[List[NewUser]] =
    userDao.listAll(CheckActiveUsers(true)).map { userSeq =>
      userSeq.toList.map { users =>
        val fullName = users.entity.email
          .split("@")
          .headOption
          .fold("Invalid") { name =>
            name.split('.').map(_.capitalize).mkString(" ")
          }
          .trim
        NewUser(
          name = fullName,
          email = users.entity.email
        )
      }
    }
}

object NewUserService {

  sealed trait NewUserServiceError

  object NewUserServiceError {
    case object UserNotFoundError extends NewUserServiceError

    case object InternalServerError extends NewUserServiceError
  }

}
