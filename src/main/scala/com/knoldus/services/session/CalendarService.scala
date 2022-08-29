package com.knoldus.services.session

import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, Or }
import com.knoldus.dao.session.CalenderDao
import com.knoldus.dao.session.CalenderDao._
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{ Admin, SuperUser }
import com.knoldus.domain.session.SessionRequest
import com.knoldus.domain.user.{ KeycloakRole, NewUserInformation }
import com.knoldus.routes.contract.session.{ AddSlotRequest, DeclineSessionRequest, UpdateApproveSessionInfo }
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.session.CalendarService.CalendarServiceError

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class CalendarService(calendarDao: CalenderDao, userDao: UserDao, mailerService: MailerService)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {

  def insertSlot(
    request: AddSlotRequest
  )(implicit authorityUser: NewUserInformation): Future[Either[CalendarServiceError, WithId[SessionRequest]]] = {
    val notification = request.isNotification
    if (authorityUser.keycloakRole == KeycloakRole.Admin)
      notification match {
        case true =>
          val sessionRequest = SessionRequest(
            approved = false,
            category = "",
            date = request.date,
            decline = false,
            email = "",
            freeSlot = false,
            meetup = false,
            notification = true,
            recommendationId = "",
            subCategory = "",
            topic = request.slotName,
            brief = ""
          )
          calendarDao.create(sessionRequest).flatMap { id =>
            Future.successful(WithId(sessionRequest, id).asRight)
          }
        case false =>
          val sessionRequest = SessionRequest(
            approved = false,
            category = "",
            date = request.date,
            decline = false,
            email = "",
            freeSlot = true,
            meetup = false,
            notification = false,
            recommendationId = "",
            subCategory = "",
            topic = request.slotName,
            brief = ""
          )
          calendarDao.create(sessionRequest).flatMap { id =>
            Future.successful(WithId(sessionRequest, id).asRight)
          }
      }
    else
      Future.successful(CalendarServiceError.AccessDenied.asLeft)
  }

  def updateSessionForApprove(
    sessionId: String,
    request: UpdateApproveSessionInfo
  ): Future[Either[CalendarServiceError, Unit]] =
    calendarDao.get(sessionId).flatMap {
      case None => Future.successful(CalendarServiceError.SlotNotFoundError.asLeft)
      case Some(_) =>
        val sessionRequest = SessionRequest(
          request.approved,
          request.category,
          request.date,
          request.decline,
          request.email,
          request.freeSlot,
          request.meetup,
          request.notification,
          request.recommendationId,
          request.subCategory,
          request.topic,
          request.brief
        )
        calendarDao.update(sessionId, _ => sessionRequest)
        Future.successful(().asRight)
    }

  def getAllFreeSlots: Future[Either[CalendarServiceError, Seq[WithId[SessionRequest]]]] =
    calendarDao.listAll(FreeSlot(true)).map { freeSlots =>
      freeSlots.asRight
    }

  def updateDateForPendingSession(sessionId: String, date: Date): Future[Either[CalendarServiceError, String]] =
    calendarDao.get(sessionId).flatMap {
      case None => Future.successful(CalendarServiceError.SessionNotFoundError.asLeft)
      case Some(session) =>
        val updatedSession = SessionRequest(
          session.entity.approved,
          session.entity.category,
          date,
          session.entity.decline,
          session.entity.email,
          session.entity.freeSlot,
          session.entity.meetup,
          session.entity.notification,
          session.entity.recommendationId,
          session.entity.subCategory,
          session.entity.topic,
          session.entity.brief
        )
        calendarDao.update(sessionId, _ => updatedSession)
        Future.successful(sessionId.asRight)
    }

  def sendEmail(sessionId: String): Future[Either[CalendarServiceError, String]] = {
    val userInfo = userDao.listAll(Or(Admin(true), SuperUser(true)))
    val adminAndSuperUserEmail = userInfo.map(user => user.map(_.entity.email))
    calendarDao.get(sessionId).flatMap { session =>
      val sessionInfo = session.get
      adminAndSuperUserEmail.map { listOfEmails =>
        val subject = s"Session requested: ${sessionInfo.entity.topic} for ${sessionInfo.entity.date}"
        val content = EmailContent.setContentForSessionReview(
          sessionInfo.entity.email,
          sessionInfo.entity.topic,
          sessionInfo.entity.date.toString,
          sessionInfo.entity.category,
          sessionInfo.entity.subCategory
        )
        mailerService.sendMessage(
          listOfEmails.toList,
          subject,
          content
        )
      }
    }
    Future.successful("".asRight)
  }

  def getAllSessionsForAdmin(
    pageNumber: Int,
    pageSize: Int,
    keyword: Option[String] = None
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[CalendarServiceError, (Seq[WithId[SessionRequest]], Int)]] =
    if (authorityUser.keycloakRole == KeycloakRole.Admin)
      keyword match {
        case Some(key) =>
          val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
          val result = for {
            sessions <- calendarDao.list(
              And(
                And(Notification(false), FreeSlot(false)),
                Or(Email(emailAndTopicSearch), Topic(emailAndTopicSearch))
              ),
              pageNumber,
              pageSize
            )
            count <- calendarDao.count(And(Notification(false), FreeSlot(false)))
          } yield (sessions, count)
          result.map { sessions =>
            sessions.asRight
          }
        case None =>
          val result = for {
            sessions <- calendarDao.list(
              And(Notification(false), FreeSlot(false)),
              pageNumber,
              pageSize
            )
            count <- calendarDao.count(And(Notification(false), FreeSlot(false)))
          } yield (sessions, count)
          result.map { sessions =>
            sessions.asRight
          }
      }
    else
      Future.successful(CalendarServiceError.AccessDenied.asLeft)

  def getPendingSessions: Future[Either[CalendarServiceError, Int]] = {
    val count = calendarDao.count(And(And(FreeSlot(false), Notification(false)), And(Approved(false), Declined(false))))
    count.map { pendingSessionsCount =>
      pendingSessionsCount.asRight
    }
  }

  def getSessionsInMonth(
    startDate: Long,
    endDate: Long
  ): Future[Either[CalendarServiceError, Seq[WithId[SessionRequest]]]] =
    calendarDao
      .listAll(
        And(And(StartDate(new Date(startDate)), EndDate(new Date(endDate))), And(Approved(false), Declined(false)))
      )
      .flatMap { sessions =>
        Future.successful(sessions.asRight)
      }

  def getSessionById(
    sessionId: String
  ): Future[Either[CalendarServiceError, WithId[SessionRequest]]] =
    calendarDao.get(sessionId).flatMap {
      case None => Future.successful(CalendarServiceError.SessionNotFoundError.asLeft)
      case Some(session) => Future.successful(session.asRight)
    }

  def declineSession(
    sessionId: String,
    request: DeclineSessionRequest
  )(implicit authorityUser: NewUserInformation): Future[Either[CalendarServiceError, WithId[SessionRequest]]] =
    if (authorityUser.keycloakRole == KeycloakRole.Admin)
      calendarDao.get(sessionId).flatMap {
        case None => Future.successful(CalendarServiceError.SessionNotFoundError.asLeft)
        case sessionInfo @ Some(session) =>
          val updatedSession = SessionRequest(
            approved = request.approved,
            category = session.entity.category,
            date = session.entity.date,
            decline = request.declined,
            email = session.entity.email,
            freeSlot = session.entity.freeSlot,
            meetup = session.entity.meetup,
            notification = session.entity.notification,
            recommendationId = session.entity.recommendationId,
            subCategory = session.entity.recommendationId,
            topic = session.entity.topic,
            brief = session.entity.brief
          )
          calendarDao.update(sessionId, _ => updatedSession)
          val addSlotRequest = new AddSlotRequest(
            "Free Slot",
            session.entity.date,
            false
          )
          insertSlot(addSlotRequest)
          Future.successful(WithId(updatedSession, sessionInfo.get.id).asRight)
      }
    else
      Future.successful(CalendarServiceError.AccessDenied.asLeft)

  def deleteSlot(id: String): Future[Either[CalendarServiceError, Unit]] =
    calendarDao.get(id).flatMap {
      case None => Future.successful(CalendarServiceError.SlotNotFoundError.asLeft)
      case _ =>
        calendarDao.delete(id)
        Future.successful(().asRight)
    }
}

object CalendarService {

  sealed trait CalendarServiceError

  object CalendarServiceError {

    case object SlotNotFoundError extends CalendarServiceError
    case object SessionNotFoundError extends CalendarServiceError
    case object AccessDenied extends CalendarServiceError

  }
}
