package com.knoldus.services.scheduler

import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao.{ CheckFeedbackScore, StoredSessionId }
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao._
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{ CheckActiveUsers, CheckBanUser, CoreMember }
import com.knoldus.domain.session.{ Session, SessionUnattendedUserInfo }
import com.knoldus.domain.user.UserInformation
import com.knoldus.routes.contract.session.GetSessionByIdResponse
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.feedbackform.FeedbackFormResponseService.FeedbackFormResponseServiceError
import com.knoldus.services.scheduler.SessionState._
import com.knoldus.services.session.SessionService.SessionServiceError

import java.time.{ LocalDate, ZoneId }
import java.util.{ Calendar, Date }
import scala.concurrent.{ ExecutionContext, Future }

class SchedulerService(
  sessionDao: SessionDao,
  feedbackFormResponseDao: FeedbackFormsResponseDao,
  userDao: UserDao
)(implicit
  val ec: ExecutionContext
) {
  val dateTimeUtility: DateTimeUtility = new DateTimeUtility

  def sessionsForToday(
    sessionState: SessionState
  ): Future[Either[SessionServiceError, List[GetSessionByIdResponse]]] = {
    val sessions = sessionState match {
      case ExpiringNext =>
        val sessions = sessionDao.listAll(
          And(
            Active(true),
            And(
              Cancelled(false),
              TodayExpiringSession(new Date(dateTimeUtility.nowMillis), new Date(dateTimeUtility.endOfDayMillis))
            )
          )
        )
        sessions

      case SchedulingNext =>
        sessionDao.listAll(
          And(
            Active(true),
            And(
              Cancelled(false),
              And(StartDate(new Date(dateTimeUtility.nowMillis)), EndDate(new Date(dateTimeUtility.endOfDayMillis)))
            )
          )
        )

      case ExpiringNextNotReminded =>
        sessionDao.listAll(
          And(
            Active(true),
            And(
              Cancelled(false),
              And(
                TodayExpiringSession(new Date(dateTimeUtility.nowMillis), new Date(dateTimeUtility.endOfDayMillis)),
                CheckReminder(false)
              )
            )
          )
        )

      case SchedulingNextUnNotified =>
        sessionDao.listAll(
          And(
            Active(true),
            And(
              Cancelled(false),
              And(
                StartDate(new Date(dateTimeUtility.nowMillis)),
                And(EndDate(new Date(dateTimeUtility.endOfDayMillis)), CheckNotification(false))
              )
            )
          )
        )
    }

    sessions.map { sessions =>
      if (sessions.nonEmpty)
        sessions
          .map { session =>
            GetSessionByIdResponse.fromDomain(session)
          }
          .toList
          .asRight
      else
        SessionServiceError.SessionNotFoundError.asLeft
    }
  }

  def updateMailStatus(sessionId: String, emailType: String): Future[Boolean] =
    sessionDao.get(sessionId).flatMap { session =>
      val sessionInformation = session.get
      val youtubeURL =
        if (sessionInformation.entity.youtubeURL.get == null) None
        else sessionInformation.entity.youtubeURL
      val slideShareURL =
        if (sessionInformation.entity.slideShareURL.get == null) None
        else sessionInformation.entity.slideShareURL

      val updateSessionInformation = emailType match {
        case "Notification" =>
          Session(
            userId = sessionInformation.entity.userId,
            email = sessionInformation.entity.email,
            date = sessionInformation.entity.date,
            sessionInformation.entity.session,
            category = sessionInformation.entity.category,
            subCategory = sessionInformation.entity.subCategory,
            feedbackFormId = sessionInformation.entity.feedbackFormId,
            topic = sessionInformation.entity.topic,
            brief = sessionInformation.entity.brief,
            feedbackExpirationDays = sessionInformation.entity.feedbackExpirationDays,
            meetup = sessionInformation.entity.meetup,
            rating = sessionInformation.entity.rating,
            score = sessionInformation.entity.score,
            cancelled = sessionInformation.entity.cancelled,
            active = sessionInformation.entity.active,
            expirationDate = sessionInformation.entity.expirationDate,
            youtubeURL = youtubeURL,
            slideShareURL = slideShareURL,
            temporaryYoutubeURL = sessionInformation.entity.temporaryYoutubeURL,
            reminder = sessionInformation.entity.reminder,
            notification = true
          )
        case "Reminder" =>
          Session(
            userId = sessionInformation.entity.userId,
            email = sessionInformation.entity.email,
            date = sessionInformation.entity.date,
            sessionInformation.entity.session,
            category = sessionInformation.entity.category,
            subCategory = sessionInformation.entity.subCategory,
            feedbackFormId = sessionInformation.entity.feedbackFormId,
            topic = sessionInformation.entity.topic,
            brief = sessionInformation.entity.brief,
            feedbackExpirationDays = sessionInformation.entity.feedbackExpirationDays,
            meetup = sessionInformation.entity.meetup,
            rating = sessionInformation.entity.rating,
            score = sessionInformation.entity.score,
            cancelled = sessionInformation.entity.cancelled,
            active = sessionInformation.entity.active,
            expirationDate = sessionInformation.entity.expirationDate,
            youtubeURL = youtubeURL,
            slideShareURL = slideShareURL,
            temporaryYoutubeURL = sessionInformation.entity.temporaryYoutubeURL,
            reminder = true,
            notification = sessionInformation.entity.notification
          )
      }
      sessionDao.update(sessionId, _ => updateSessionInformation).map {
        case Some(_) => true
        case None => false
      }
    }

  def getSessionById(sessionId: String): Future[Either[SessionServiceError, GetSessionByIdResponse]] =
    sessionDao.get(sessionId).map { session =>
      if (session.isEmpty)
        SessionServiceError.SessionNotFoundError.asLeft
      else {
        val sessionInfo = session.get
        GetSessionByIdResponse.fromDomain(sessionInfo).asRight
      }
    }

  def getUsersNotAttendedLastMonthSession: Future[List[SessionUnattendedUserInfo]] = {
    val currentDate = LocalDate.now()
    val previousMonthDate = currentDate.minusMonths(1)
    val startDate = new Date(previousMonthDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toEpochSecond * 1000)
    val endDate = new Date(currentDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toEpochSecond * 1000)
    getSessionsInMonth(startDate, endDate).flatMap { listOfSession =>
      Future.sequence(
        listOfSession.map { sessionWithId =>
          getAllNotAttendingResponseEmailsPerSession(sessionWithId.id)
            .map {
              case Right(emails) => emails
              case Left(_) => List()
            }
            .map { emails =>
              SessionUnattendedUserInfo(sessionWithId.entity.topic, emails)
            }
        }
      )
    }
  }

  def getSessionsInMonth(startDate: Date, endDate: Date): Future[List[WithId[Session]]] =
    sessionDao
      .listAll(And(And(StartDate(startDate), EndDate(endDate)), And(Active(true), SessionExpiryDate(endDate))))
      .map { sessions =>
        sessions.map { session =>
          session
        }.toList
      }

  def getAllNotAttendingResponseEmailsPerSession(
    sessionId: String
  ): Future[Either[FeedbackFormResponseServiceError, List[String]]] =
    feedbackFormResponseDao.listAll(And(CheckFeedbackScore(0d), StoredSessionId(sessionId))).map {
      feedbackFormResponses =>
        if (feedbackFormResponses.nonEmpty)
          feedbackFormResponses
            .map { feedback =>
              feedback.entity.email
            }
            .toList
            .asRight
        else
          FeedbackFormResponseServiceError.FeedbackNotFoundError.asLeft
    }

  def getAllBannedUsersOfLastMonth: Future[Seq[UserInformation]] =
    userDao.listAll(CheckActiveUsers(true)).map { activeUsers =>
      val allBannedUsersOfLastMonth = activeUsers.filter { user =>
        getLastBanMonthOfUser(user.entity) == getCurrentMonth - 1
      }
      allBannedUsersOfLastMonth.map(_.entity)
    }

  private def getLastBanMonthOfUser(userInformation: UserInformation): Int = {
    val calendar = Calendar.getInstance()
    val lastBannedOn = userInformation.lastBannedOn match {
      case Some(date) => date
      case None => new Date(dateTimeUtility.nowMillis)
    }
    calendar.setTime(lastBannedOn)
    calendar.get(Calendar.MONTH)
  }

  private def getCurrentMonth: Int =
    Calendar.getInstance().get(Calendar.MONTH)

  def getAllBannedUsers: Future[Seq[UserInformation]] =
    userDao.listAll(And(CheckBanUser(new Date(dateTimeUtility.nowMillis)), CheckActiveUsers(true))).map {
      _.map {
        _.entity
      }
    }

  def getAllCoreMembers: Future[Seq[UserInformation]] =
    userDao.listAll(And(CoreMember(true), CheckActiveUsers(true))).map {
      _.map {
        _.entity
      }
    }

}
