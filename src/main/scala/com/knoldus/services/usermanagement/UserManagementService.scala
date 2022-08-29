package com.knoldus.services.usermanagement

import akka.Done
import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, TrueFilter }
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{
  ActiveEmailCheck,
  CheckActiveUsers,
  CheckBanUser,
  CheckUnBanUser,
  EmailCheck,
  EmailFilter
}
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.routes.contract.usermanagement._
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.usermanagement.UserManagementService._
import com.knoldus.services.usermanagement.utilities.PasswordUtility
import com.typesafe.config.Config

import java.time.LocalDateTime
import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure

class UserManagementService(
  userDao: UserDao,
  passwordUtility: PasswordUtility,
  conf: Config,
  mailerService: MailerService
)(implicit
  val ec: ExecutionContext,
  logger: LoggingAdapter
) {

  val dateTimeUtility = new DateTimeUtility
  val banPeriod = 30
  val frontendURL: String = conf.getConfig("frontendURL").getString("ui-url")

  def deleteUser(
    userEmail: String
  )(implicit authorityUser: NewUserInformation): Future[Either[UserManagementServiceError, String]] = {
    val email = userEmail.toLowerCase.trim
    if (authorityUser.keycloakRole == Admin)
      userDao.get(ActiveEmailCheck(email)).map {
        case None => UserManagementServiceError.UserNotFoundError.asLeft
        case Some(user) =>
          val lastBannedOn = user.entity.lastBannedOn
          val userInfo = UserInformation(
            user.entity.email,
            active = false,
            admin = false,
            coreMember = user.entity.coreMember,
            superUser = user.entity.superUser,
            user.entity.banTill,
            user.entity.banCount,
            lastBannedOn,
            user.entity.nonParticipating,
            user.entity.department
          )
          userDao.update(user.id, _ => userInfo)
          "Deleted".asRight
      }
    else
      Future.successful(UserManagementServiceError.AccessDenied.asLeft)
  }

  def getUserStatusByEmail(email: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[UserManagementServiceError, UserStatusInformationResponse]] =
    if (authorityUser.keycloakRole == Admin)
      userDao.get(EmailCheck(email)).map {
        case None => UserManagementServiceError.UserNotFoundError.asLeft
        case Some(user) =>
          val userInformation = user.entity
          val ban = userInformation.banTill.after(new Date(dateTimeUtility.nowMillis))
          UserStatusInformationResponse(
            userInformation.email,
            userInformation.active,
            ban,
            userInformation.coreMember,
            userInformation.admin,
            None
          ).asRight
      }
    else
      Future.successful(UserManagementServiceError.AccessDenied.asLeft)

  def updateUserStatus(
    updateUserStatusRequest: UpdateUserStatusRequest
  )(implicit authorityUser: NewUserInformation): Future[Either[UserManagementServiceError, String]] =
    if (authorityUser.keycloakRole == Admin) {
      val email = updateUserStatusRequest.email
      userDao.get(EmailCheck(updateUserStatusRequest.email)).flatMap {
        case None => Future.successful(UserManagementServiceError.UserNotFoundError.asLeft)
        case Some(user) =>
          val userInformation = user.entity
          val banUser = checkBanUserOrNot(email)
          val lastBannedOn = userInformation.lastBannedOn
          val banTill: LocalDateTime = dateTimeUtility.toLocalDateTime(dateTimeUtility.nowMillis).plusDays(banPeriod)
          val duration = new Date(dateTimeUtility.toMillis(banTill))
          val unBan = new Date(dateTimeUtility.nowMillis)
          val ban = if (userInformation.banTill.after(unBan)) userInformation.banTill else duration
          val admin = if (userInformation.admin) true else updateUserStatusRequest.admin

          val updateUser = (updateUserStatusRequest.password, updateUserStatusRequest.ban) match {
            case (Some(password), true) =>
              UserInformation(
                userInformation.email,
                updateUserStatusRequest.active,
                admin,
                updateUserStatusRequest.coreMember,
                userInformation.superUser,
                ban,
                userInformation.banCount,
                lastBannedOn,
                user.entity.nonParticipating,
                user.entity.department
              )
            case (Some(password), false) =>
              UserInformation(
                userInformation.email,
                updateUserStatusRequest.active,
                admin,
                updateUserStatusRequest.coreMember,
                userInformation.superUser,
                unBan,
                userInformation.banCount,
                lastBannedOn,
                userInformation.nonParticipating,
                user.entity.department
              )
            case (None, true) =>
              UserInformation(
                userInformation.email,
                updateUserStatusRequest.active,
                admin,
                updateUserStatusRequest.coreMember,
                userInformation.superUser,
                ban,
                userInformation.banCount,
                lastBannedOn,
                userInformation.nonParticipating,
                user.entity.department
              )
            case (None, false) =>
              UserInformation(
                userInformation.email,
                updateUserStatusRequest.active,
                admin,
                updateUserStatusRequest.coreMember,
                userInformation.superUser,
                unBan,
                userInformation.banCount,
                lastBannedOn,
                userInformation.nonParticipating,
                user.entity.department
              )
          }
          banUser.flatMap { banned =>
            (banned, updateUserStatusRequest.ban) match {
              case (true, false) =>
                userDao.update(user.id, _ => updateUser).flatMap { _ =>
                  sendUserUnbannedNotificationMail(updateUserStatusRequest.email).map(_ => "updated".asRight)
                }

              case _ =>
                userDao.update(user.id, _ => updateUser).map { _ =>
                  "updated".asRight
                }
            }
          }
      }
    } else
      Future.successful(UserManagementServiceError.AccessDenied.asLeft)

  def sendUserUnbannedNotificationMail(unbannedEmail: String): Future[Done] = {
    val result = mailerService.sendMessage(
      unbannedEmail,
      "User Unbanned Notification",
      EmailContent.setUserUnbannedEmailContent(unbannedEmail)
    )
    result.andThen {
      case Failure(exception) => logger.error(exception.toString)
    }
    result.recover {
      case _ => Done
    }
  }

  def checkBanUserOrNot(email: String): Future[Boolean] = {
    def getBanUserIds(userData: Seq[WithId[UserInformation]]): Future[Seq[String]] =
      Future.successful(userData.map { user =>
        user.entity.email
      })

    def checkIfIdContains(ids: Seq[String]): Future[Boolean] =
      if (ids.contains(email))
        Future.successful(true)
      else
        Future.successful(false)

    for {
      userInfo <- userDao.listAll(CheckBanUser(new Date()))
      banUserIds <- getBanUserIds(userInfo)
      banUserInfo <- checkIfIdContains(banUserIds)
    } yield banUserInfo

  }

  def searchUsers(
    pageNumber: Int,
    pageSize: Int = 10,
    filter: String = "all",
    email: Option[String] = None
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[UserManagementServiceError, (Seq[SearchUserResponse], Int)]] =
    getUsers(pageNumber, pageSize, filter, email).map {
      case Right((users, count)) if authorityUser.keycloakRole == Admin =>
        val usersList = users.map {
          case WithId(user, id) =>
            SearchUserResponse(
              user.email,
              user.active,
              id,
              user.banTill.toString,
              user.admin,
              user.superUser,
              user.coreMember,
              user.banTill.after(new Date(dateTimeUtility.nowMillis)),
              user.department
            )
        }
        (usersList, count).asRight
      case Right(_) => UserManagementServiceError.AccessDenied.asLeft
      case Left(error) => error.asLeft
    }

  def getUsers(
    pageNumber: Int,
    pageSize: Int,
    filter: String,
    email: Option[String]
  ): Future[Either[UserManagementServiceError, (Seq[WithId[UserInformation]], Int)]] = {
    (email, filter) match {
      case (Some(keyword), "all") =>
        val key = ".*" + keyword.replaceAll("\\s", "").toLowerCase + ".*"
        val result = for {
          user <- userDao.list(
            And(EmailFilter(key), CheckActiveUsers(true)),
            pageNumber,
            pageSize
          )
          count <- userDao.count(EmailFilter(key))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (Some(keyword), "banned") =>
        val key = ".*" + keyword.replaceAll("\\s", "").toLowerCase + ".*"
        val result = for {
          user <- userDao.list(
            And(EmailFilter(key), And(CheckActiveUsers(true), CheckBanUser(new Date(dateTimeUtility.nowMillis)))),
            pageNumber,
            pageSize
          )
          count <- userDao.count(And(EmailFilter(key), CheckBanUser(new Date(dateTimeUtility.nowMillis))))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (Some(keyword), "allowed") =>
        val key = ".*" + keyword.replaceAll("\\s", "").toLowerCase + ".*"
        val result = for {
          user <- userDao.list(
            And(EmailFilter(key), And(CheckActiveUsers(true), CheckUnBanUser(new Date(dateTimeUtility.nowMillis)))),
            pageNumber,
            pageSize
          )
          count <- userDao.count(And(EmailFilter(key), CheckUnBanUser(new Date(dateTimeUtility.nowMillis))))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (Some(keyword), "active") =>
        val key = ".*" + keyword.replaceAll("\\s", "").toLowerCase + ".*"
        val result = for {
          user <- userDao.list(
            And(EmailFilter(key), CheckActiveUsers(true)),
            pageNumber,
            pageSize
          )
          count <- userDao.count(And(EmailFilter(key), CheckActiveUsers(true)))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (Some(keyword), "suspended") =>
        val key = ".*" + keyword.replaceAll("\\s", "").toLowerCase + ".*"
        val result = for {
          user <- userDao.list(
            And(
              EmailFilter(key),
              CheckActiveUsers(false)
            ),
            pageNumber,
            pageSize
          )
          count <- userDao.count(And(EmailFilter(key), CheckActiveUsers(false)))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (None, "all") =>
        val result = for {
          user <- userDao.list(
            CheckActiveUsers(true),
            pageNumber,
            pageSize
          )
          count <- userDao.count(TrueFilter)
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (None, "banned") =>
        val result = for {
          user <- userDao.list(
            And(CheckActiveUsers(true), CheckBanUser(new Date(dateTimeUtility.nowMillis))),
            pageNumber,
            pageSize
          )
          count <- userDao.count(CheckBanUser(new Date(dateTimeUtility.nowMillis)))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (None, "allowed") =>
        val result = for {
          user <- userDao.list(
            And(CheckActiveUsers(true), CheckUnBanUser(new Date(dateTimeUtility.nowMillis))),
            pageNumber,
            pageSize
          )
          count <- userDao.count(CheckUnBanUser(new Date(dateTimeUtility.nowMillis)))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (None, "active") =>
        val result = for {
          user <- userDao.list(
            CheckActiveUsers(true),
            pageNumber,
            pageSize
          )
          count <- userDao.count(CheckActiveUsers(true))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case (None, "suspended") =>
        val result = for {
          user <- userDao.list(
            CheckActiveUsers(false),
            pageNumber,
            pageSize
          )
          count <- userDao.count(CheckActiveUsers(false))
        } yield (user, count)
        result.map { response =>
          response.asRight
        }
      case _ =>
        Future.successful(UserManagementServiceError.UserNotFoundError.asLeft)
    }
  }

  def usersList(email: Option[String] = None): Future[Either[UserManagementServiceError, Seq[String]]] = {
    val listOfUsers = email match {
      case None =>
        userDao.listAll(CheckActiveUsers(true))

      case Some(keyword) => userDao.listAll(ActiveEmailCheck(keyword))
    }

    listOfUsers.flatMap { users =>
      val list = users.map { user =>
        user.entity.email
      }
      Future.successful(list.asRight)
    }
  }

  def changeParticipantStatus(
    userEmail: String,
    changeParticipantStatus: ChangeParticipantStatusResponse
  )(implicit authorityUser: NewUserInformation): Future[Either[UserManagementServiceError, akka.Done]] = {
    val email = userEmail.toLowerCase.trim
    if (email == authorityUser.email)
      userDao.get(ActiveEmailCheck(email)).map {
        case None => UserManagementServiceError.UserNotFoundError.asLeft
        case Some(userInfoWithId) =>
          val userInfo = userInfoWithId.entity
          val lastBannedOn = userInfo.lastBannedOn
          val updateUserInfo = UserInformation(
            userInfo.email,
            userInfo.active,
            userInfo.admin,
            userInfo.coreMember,
            userInfo.superUser,
            userInfo.banTill,
            userInfo.banCount,
            lastBannedOn,
            changeParticipantStatus.nonParticipating,
            userInfo.department
          )
          userDao.update(userInfoWithId.id, _ => updateUserInfo)
          akka.Done.asRight
      }
    else
      Future.successful(UserManagementServiceError.AccessDenied.asLeft)
  }

  def getActiveAndUnBannedEmails: Future[Either[UserManagementServiceError.UserNotFoundError.type, List[String]]] =
    userDao.listAll(And(CheckActiveUsers(true), CheckUnBanUser(new Date(dateTimeUtility.nowMillis)))).map { users =>
      if (users.nonEmpty)
        users
          .map { user =>
            user.entity.email
          }
          .toList
          .asRight
      else
        UserManagementServiceError.UserNotFoundError.asLeft
    }

  def banUser(email: String): Future[Boolean] =
    userDao.get(EmailCheck(email)).flatMap { user =>
      val userData = user.get.entity
      val banTill: LocalDateTime = dateTimeUtility.toLocalDateTime(dateTimeUtility.nowMillis).plusDays(banPeriod)
      val userDataToUpdate = UserInformation(
        userData.email,
        userData.active,
        userData.admin,
        userData.coreMember,
        userData.superUser,
        new Date(dateTimeUtility.toMillis(banTill)),
        userData.banCount + 1,
        Some(new Date(dateTimeUtility.nowMillis)),
        userData.nonParticipating,
        userData.department
      )
      userDao.update(user.get.id, _ => userDataToUpdate).map {
        case Some(_) => true
        case None => false
      }
    }

  def getUserInformationByEmail(email: String): Future[Either[UserManagementServiceError, UserInformationResponse]] =
    userDao.get(EmailCheck(email)).map {
      case Some(user) =>
        val userInformation = UserInformationResponse(
          user.entity.email,
          user.entity.active,
          user.entity.admin,
          user.entity.coreMember,
          user.entity.superUser,
          user.entity.banTill,
          user.entity.banCount,
          user.id,
          user.entity.department
        )
        userInformation.asRight
      case None => UserManagementServiceError.UserNotFoundError.asLeft
    }

  def updateUserDepartment(
    email: String,
    newDepartment: String
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[UserManagementServiceError, Done]] =
    if (authorityUser.email == email)
      userDao.get(EmailCheck(email)).flatMap {
        case Some(WithId(user, id)) =>
          val updatedUser = user.copy(department = Some(newDepartment))
          userDao.update(id, _ => updatedUser).map {
            case Some(_) => Done.asRight
            case None => UserManagementServiceError.InternalServerError.asLeft
          }
        case None => Future.successful(UserManagementServiceError.UserNotFoundError.asLeft)
      }
    else Future.successful(UserManagementServiceError.AccessDenied.asLeft)

  def userDepartmentUpdater(user: UserInformation): UserInformation => UserInformation = { _: UserInformation =>
    user
  }
}

object UserManagementService {

  sealed trait UserManagementServiceError

  object UserManagementServiceError {

    case object UserNotFoundError extends UserManagementServiceError

    case object EmailAlreadyExist extends UserManagementServiceError

    case object InvalidCredentials extends UserManagementServiceError

    case object TokenExpired extends UserManagementServiceError

    case object NoForgotPasswordRequestFound extends UserManagementServiceError

    case object AccessDenied extends UserManagementServiceError

    case object UserDeactivated extends UserManagementServiceError

    case object UserNotActivated extends UserManagementServiceError

    case object Unauthorized extends UserManagementServiceError

    case object InternalServerError extends UserManagementServiceError

  }

}
