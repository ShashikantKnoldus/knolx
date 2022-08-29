package com.knoldus.services.session

import java.time.DayOfWeek
import java.util.Date
import akka.actor.ActorRef
import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, Or }
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao._
import com.knoldus.dao.sorting.Direction.Descending
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{ EmailCheck, NonParticipatingUsers }
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.session.{ CreateSessionRequest, UpcomingSession }
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.scheduler.SessionScheduler.{ RefreshSessionsSchedulers, ScheduleSession }
import com.knoldus.services.session.SessionService.SessionServiceError

import scala.concurrent._

class SessionService(
  sessionDao: SessionDao,
  userDao: UserDao,
  sessionScheduler: ActorRef,
  mailerService: MailerService
)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {
  val dateTimeUtility = new DateTimeUtility

  def createSession(
    request: CreateSessionRequest
  )(implicit authorityUser: NewUserInformation): Future[Either[SessionServiceError, WithId[Session]]] = {
    val email = request.email.toLowerCase
    if (authorityUser.keycloakRole == Admin)
      userDao.get(EmailCheck(email) && NonParticipatingUsers(false)).flatMap { userInfo =>
        if (userInfo.isEmpty)
          Future.successful(SessionServiceError.EmailNotFoundError.asLeft)
        else {
          val expirationDateMillis = sessionExpirationMillis(
            request.date,
            request.feedbackExpirationDays
          )
          val userId = userInfo.get.id
          val session = Session(
            userId,
            email = request.email,
            date = request.date,
            request.session,
            category = request.category,
            subCategory = request.subCategory,
            feedbackFormId = request.feedbackFormId,
            topic = request.topic,
            feedbackExpirationDays = request.feedbackExpirationDays,
            meetup = request.meetup,
            brief = request.brief,
            rating = "",
            score = 0.0,
            cancelled = false,
            active = true,
            new Date(expirationDateMillis),
            None,
            None,
            None,
            reminder = false,
            notification = false
          )

          sessionDao.create(session).flatMap { id =>
            val sessionInfo = WithId(session, id)
            sessionScheduler ! RefreshSessionsSchedulers
            Future.successful(sessionInfo.asRight)
          }

        }
      }
    else
      Future.successful(SessionServiceError.AccessDenied.asLeft)
  }

  def getSessions(
    pageNumber: Int,
    pageSize: Int,
    filter: String,
    email: Option[String] = None,
    category: Option[String] = None
  ): Future[Either[SessionServiceError, (Seq[WithId[Session]], Int)]] =
    filter match {
      case "all" =>
        getAllSessions(pageNumber, pageSize, email).map(sessions =>
          sessions.flatMap(sessionInfo => sessionInfo.asRight)
        )
      case "upcoming" =>
        getPendingSessions(pageNumber, pageSize, email).map(sessions =>
          sessions.flatMap(sessionInfo => sessionInfo.asRight)
        )
      case "completed" =>
        getCompletedSessions(pageNumber, pageSize, email).map(sessions =>
          sessions.flatMap(sessionInfo => sessionInfo.asRight)
        )
      case "category" =>
        category match {
          case Some(value) =>
            getSessionByCategory(pageNumber, pageSize, value).map(sessions =>
              sessions.flatMap(sessionInfo => sessionInfo.asRight)
            )
          case _ =>
            getAllSessions(pageNumber, pageSize, email).map(sessions =>
              sessions.flatMap(sessionInfo => sessionInfo.asRight)
            )
        }
    }

  def getSessionByCategory(
    pageNumber: Int,
    pageSize: Int,
    category: String
  ): Future[Either[SessionServiceError, (Seq[WithId[Session]], Int)]] = {
    val result = for {
      sessions <- sessionDao.list(
        And(Active(true), CategoryCheck(category)),
        pageNumber,
        pageSize,
        Some(SortBy(fields = (DateMatch, Descending)))
      )
      count <- sessionDao.count(And(Active(true), CategoryCheck(category)))
    } yield (sessions, count)
    result.map { response =>
      response.asRight
    }
  }

  def getAllSessions(
    pageNumber: Int,
    pageSize: Int,
    email: Option[String] = None
  ): Future[Either[SessionServiceError, (Seq[WithId[Session]], Int)]] =
    email match {
      case Some(key) =>
        val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
        val result = for {
          sessions <- sessionDao.list(
            And(
              And(Active(true), Completed(new Date)),
              Or(Email(emailAndTopicSearch), Topic(emailAndTopicSearch))
            ),
            pageNumber,
            pageSize,
            Some(SortBy((DateMatch, Descending)))
          )
          count <- sessionDao.count(
            And(
              And(Active(true), Completed(new Date)),
              Or(Email(emailAndTopicSearch), Topic(emailAndTopicSearch))
            )
          )
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
      case _ =>
        val result = for {
          sessions <- sessionDao.list(
            And(Active(true), Completed(new Date)),
            pageNumber,
            pageSize,
            Some(SortBy(fields = (DateMatch, Descending)))
          )
          count <- sessionDao.count(And(Active(true), Completed(new Date)))
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
    }

  def getPendingSessions(
    pageNumber: Int,
    pageSize: Int,
    email: Option[String] = None
  ): Future[Either[SessionServiceError, (Seq[WithId[Session]], Int)]] =
    email match {
      case Some(key) =>
        val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
        val result = for {
          sessions <- sessionDao.list(
            And(
              And(And(Active(true), Pending(new Date)), CancelledSession(false)),
              Or(Email(emailAndTopicSearch), Topic(emailAndTopicSearch))
            ),
            pageNumber,
            pageSize,
            Some(SortBy(fields = (DateMatch, Descending)))
          )
          count <- sessionDao.count(And(And(Active(true), Pending(new Date)), CancelledSession(false)))
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
      case _ =>
        val result = for {
          sessions <- sessionDao.list(
            And(And(Active(true), Pending(new Date)), CancelledSession(false)),
            pageNumber,
            pageSize,
            Some(SortBy(fields = (DateMatch, Descending)))
          )
          count <- sessionDao.count(And(And(Active(true), Pending(new Date)), CancelledSession(false)))
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
    }

  def getCompletedSessions(
    pageNumber: Int,
    pageSize: Int,
    email: Option[String] = None
  ): Future[Either[SessionServiceError, (Seq[WithId[Session]], Int)]] =
    email match {
      case Some(key) =>
        val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
        val result = for {
          sessions <- sessionDao.list(
            And(
              And(And(Active(true), Completed(new Date)), CancelledSession(false)),
              Or(Email(emailAndTopicSearch), Topic(emailAndTopicSearch))
            ),
            pageNumber,
            pageSize,
            Some(SortBy(fields = (DateMatch, Descending)))
          )
          count <- sessionDao.count(And(And(Active(true), Completed(new Date)), CancelledSession(false)))
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
      case _ =>
        val result = for {
          sessions <- sessionDao.list(
            And(And(Active(true), Completed(new Date)), CancelledSession(false)),
            pageNumber,
            pageSize,
            Some(SortBy(fields = (DateMatch, Descending)))
          )
          count <- sessionDao.count(And(And(Active(true), Completed(new Date)), CancelledSession(false)))
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
    }

  def delete(id: String)(implicit authorityUser: NewUserInformation): Future[Either[SessionServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin)
      sessionDao.get(id).flatMap { sessions =>
        sessions match {
          case Some(sessionInfo) =>
            val youtubeURL =
              sessionInfo.entity.youtubeURL
            val slideShareURL =
              sessionInfo.entity.slideShareURL
            val temporaryYoutubeURL =
              sessionInfo.entity.temporaryYoutubeURL
            val session = Session(
              userId = sessionInfo.entity.userId,
              email = sessionInfo.entity.email,
              date = sessionInfo.entity.date,
              sessionInfo.entity.session,
              category = sessionInfo.entity.category,
              subCategory = sessionInfo.entity.subCategory,
              feedbackFormId = sessionInfo.entity.feedbackFormId,
              topic = sessionInfo.entity.topic,
              feedbackExpirationDays = sessionInfo.entity.feedbackExpirationDays,
              meetup = sessionInfo.entity.meetup,
              brief = sessionInfo.entity.brief,
              rating = sessionInfo.entity.rating,
              score = sessionInfo.entity.score,
              cancelled = sessionInfo.entity.cancelled,
              active = false,
              expirationDate = sessionInfo.entity.expirationDate,
              youtubeURL = youtubeURL,
              slideShareURL = slideShareURL,
              temporaryYoutubeURL = temporaryYoutubeURL,
              reminder = sessionInfo.entity.reminder,
              notification = sessionInfo.entity.notification
            )
            sessionDao.update(id, _ => session)
            sessionScheduler ! RefreshSessionsSchedulers
            Future.successful(().asRight)

          case None => Future.successful(SessionServiceError.SessionNotFoundError.asLeft)
        }
      }
    else
      Future.successful(SessionServiceError.AccessDenied.asLeft)

  def getSession(
    id: String
  ): Future[Either[SessionServiceError, WithId[Session]]] =
    sessionDao.get(id).flatMap { session =>
      if (session.isEmpty)
        Future.successful(SessionServiceError.SessionNotFoundError.asLeft)
      else {
        val sessionInfo = session.get
        Future.successful(sessionInfo.asRight)

      }
    }

  def getUpcomingSessions(
    email: Option[String] = None
  ): Future[Either[SessionServiceError, Seq[UpcomingSession]]] =
    email match {
      case Some(key) =>
        val email = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
        val result = for {
          sessions <- sessionDao.listAll(
            And(And(StartDate(new Date), Email(email)), And(Cancelled(false), Active(true)))
          )
        } yield sessions
        val upcomingSessionsResponse = result.map { seqOfSessions =>
          val emailArray: Array[String] = key.split("@")
          val name = emailArray(0).split('.').map(_.capitalize).mkString(" ")
          val upcomingSessionsSeq = seqOfSessions.map { entry =>
            val upcomingSession = UpcomingSession(
              "Knolx",
              entry.entity.category,
              entry.entity.topic,
              entry.entity.email,
              name,
              entry.entity.date
            )
            if (entry.entity.meetup) {
              val newUpcomingSession = upcomingSession.copy(label = "Meetup")
              newUpcomingSession
            } else
              upcomingSession
          }
          upcomingSessionsSeq.asRight
        }
        upcomingSessionsResponse
      case _ =>
        val result = for {
          sessions <- sessionDao.listAll(
            And(StartDate(new Date), And(Cancelled(false), Active(true)))
          )
        } yield sessions
        val upcomingSessionsResponse = result.map { seqOfSessions =>
          val upcomingSessionsSeq = seqOfSessions.map { entry =>
            val emailArray: Array[String] = entry.entity.email.split("@")
            val name = emailArray(0).split('.').map(_.capitalize).mkString(" ")
            val upcomingSession = UpcomingSession(
              "Knolx",
              entry.entity.category,
              entry.entity.topic,
              entry.entity.email,
              name,
              entry.entity.date
            )
            if (entry.entity.meetup) {
              val newUpcomingSession = upcomingSession.copy(label = "Meetup")
              newUpcomingSession
            } else
              upcomingSession
          }
          upcomingSessionsSeq.asRight
        }
        upcomingSessionsResponse
    }

  def getSessionInMonth(startDate: Long, endDate: Long): Future[Either[SessionServiceError, Seq[WithId[Session]]]] =
    sessionDao.listAll(And(And(StartDate(new Date(startDate)), EndDate(new Date(endDate))), Active(true))).flatMap {
      sessions =>
        Future.successful(sessions.asRight)
    }

  def update(
    id: String,
    session: Option[String] = None,
    date: Option[Date] = None,
    category: Option[String] = None,
    subCategory: Option[String] = None,
    feedbackFormId: Option[String] = None,
    topic: Option[String] = None,
    brief: Option[String] = None,
    feedbackExpirationDays: Option[Int] = None,
    youtubeURL: Option[String] = None,
    slideShareURL: Option[String] = None,
    cancelled: Option[Boolean] = None,
    meetup: Option[Boolean] = None
  )(implicit authorityUser: NewUserInformation): Future[Either[SessionServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin)
      sessionDao.get(id).flatMap { sessions =>
        if (sessions.isEmpty)
          Future.successful(SessionServiceError.SessionNotFoundError.asLeft)
        else {
          val expirationDate = sessionExpirationMillis(date.get, feedbackExpirationDays.get)
          val sessionInfo = sessions.get
          val updateSessionInfo = Session(
            userId = sessionInfo.entity.userId,
            email = sessionInfo.entity.email,
            date.get,
            session.get,
            category.get,
            subCategory.get,
            feedbackFormId.get,
            topic.get,
            feedbackExpirationDays.get,
            meetup.get,
            brief.get,
            rating = sessionInfo.entity.rating,
            score = sessionInfo.entity.score,
            cancelled.get,
            active = sessionInfo.entity.active,
            new Date(expirationDate),
            youtubeURL,
            slideShareURL,
            temporaryYoutubeURL = sessionInfo.entity.temporaryYoutubeURL,
            reminder = sessionInfo.entity.reminder,
            notification = sessionInfo.entity.notification
          )
          sessionDao.update(id, _ => updateSessionInfo)
          sessionScheduler ! RefreshSessionsSchedulers
          Future.successful(().asRight)
        }
      }
    else
      Future.successful(SessionServiceError.AccessDenied.asLeft)

  private def sessionExpirationMillis(date: Date, customDays: Int): Long =
    if (customDays > 0)
      customSessionExpirationMillis(date, customDays)
    else
      defaultSessionExpirationMillis(date)

  private def defaultSessionExpirationMillis(date: Date): Long = {
    val scheduledDate = dateTimeUtility.toLocalDateTimeEndOfDay(date)
    val expirationDate = scheduledDate.getDayOfWeek match {
      case DayOfWeek.FRIDAY => scheduledDate.plusDays(4)
      case DayOfWeek.SATURDAY => scheduledDate.plusDays(3)
      case _: DayOfWeek => scheduledDate.plusDays(1)
    }

    dateTimeUtility.toMillis(expirationDate)
  }

  private def customSessionExpirationMillis(date: Date, days: Int): Long = {
    val scheduledDate = dateTimeUtility.toLocalDateTimeEndOfDay(date)
    val expirationDate = scheduledDate.plusDays(days)

    dateTimeUtility.toMillis(expirationDate)
  }

  def sendEmailToPresenter(
    sessionId: String
  )(implicit authorityUser: NewUserInformation): Future[Either[SessionServiceError, String]] = {
    val sessionInfo = sessionDao.get(sessionId).map(_.get)
    val filteredSession = sessionInfo.filter(session => session.id == sessionId)

    if (authorityUser.keycloakRole == Admin) {
      filteredSession.map { session =>
        val content: String = EmailContent.setContentForPresenter(session.entity.topic, session.entity.date.toString)
        mailerService.sendMessage(
          session.entity.email,
          "Session Scheduled!",
          content
        )
      }
      Future.successful("".asRight)
    } else
      Future.successful(SessionServiceError.AccessDenied.asLeft)
  }

  def scheduleSession(
    sessionId: String
  )(implicit authorityUser: NewUserInformation): Future[Either[SessionServiceError, String]] =
    if (authorityUser.keycloakRole == Admin) {
      sessionScheduler ! ScheduleSession(sessionId)
      Future.successful("Scheduled Successfully".asRight)
    } else
      Future.successful(SessionServiceError.AccessDenied.asLeft)

  def storeTemporaryURL(sessionId: String, videoURL: String): Future[Either[SessionServiceError, String]] =
    sessionDao.get(sessionId).map {
      case None =>
        SessionServiceError.SessionNotFoundError.asLeft
      case Some(sessionInfo) =>
        val youtubeURL = sessionInfo.entity.youtubeURL
        val slideShareURL = sessionInfo.entity.slideShareURL

        val session = Session(
          userId = sessionInfo.entity.userId,
          email = sessionInfo.entity.email,
          date = sessionInfo.entity.date,
          sessionInfo.entity.session,
          category = sessionInfo.entity.category,
          subCategory = sessionInfo.entity.subCategory,
          feedbackFormId = sessionInfo.entity.feedbackFormId,
          topic = sessionInfo.entity.topic,
          feedbackExpirationDays = sessionInfo.entity.feedbackExpirationDays,
          meetup = sessionInfo.entity.meetup,
          brief = sessionInfo.entity.brief,
          rating = sessionInfo.entity.rating,
          score = sessionInfo.entity.score,
          cancelled = sessionInfo.entity.cancelled,
          active = sessionInfo.entity.active,
          expirationDate = sessionInfo.entity.expirationDate,
          youtubeURL = youtubeURL,
          slideShareURL = slideShareURL,
          temporaryYoutubeURL = Some(videoURL),
          reminder = sessionInfo.entity.reminder,
          notification = sessionInfo.entity.notification
        )
        sessionDao.update(sessionId, _ => session)
        "URL stored".asRight
    }

  def getTemporaryVideoURL(sessionId: String): Future[Either[SessionServiceError, String]] =
    sessionDao.get(sessionId).map {
      case None => SessionServiceError.SessionNotFoundError.asLeft
      case Some(sessionInfo) => sessionInfo.entity.temporaryYoutubeURL.getOrElse("").asRight
    }

  def getYoutubeVideoURL(sessionId: String): Future[Either[SessionServiceError, String]] =
    sessionDao.get(sessionId).map {
      case None => SessionServiceError.SessionNotFoundError.asLeft
      case Some(sessionInfo) => sessionInfo.entity.youtubeURL.getOrElse("").asRight
    }
}

object SessionService {

  sealed trait SessionServiceError

  object SessionServiceError {

    case object AccessDenied extends SessionServiceError

    case object SessionNotFoundError extends SessionServiceError

    case object EmailNotFoundError extends SessionServiceError

  }

}
