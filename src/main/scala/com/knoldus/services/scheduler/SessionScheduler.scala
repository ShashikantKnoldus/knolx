package com.knoldus.services.scheduler

import akka.actor.{ Actor, Cancellable, Scheduler }
import akka.event.LoggingAdapter
import com.knoldus.routes.contract.session.GetSessionByIdResponse
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.feedbackform.FeedbackFormResponseService
import com.knoldus.services.scheduler.SessionScheduler._
import com.knoldus.services.scheduler.SessionState._
import com.knoldus.services.usermanagement.UserManagementService
import com.typesafe.config.Config

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure

object SessionScheduler {

  case object RefreshSessionsSchedulers

  case object ScheduleFeedbackRemindersStartingTomorrow

  case object ScheduleSessionNotificationStartingTomorrow

  case object ScheduleFeedbackEmailsStartingTomorrow

  final case class ScheduleSession(sessionId: String)

  final case class SendMail(session: List[GetSessionByIdResponse], emailType: EmailType)

  final case class EmailInfo(topic: String, presenter: String, date: String)

  final case class ScheduleFeedbackRemindersStartingToday(eventualSessions: Future[List[GetSessionByIdResponse]])

  final case class InitialFeedbackRemindersStartingTomorrow(initialDelay: FiniteDuration, interval: FiniteDuration)

  final case class ScheduleSessionNotificationsStartingToday(
    eventualSessions: Future[List[GetSessionByIdResponse]]
  )

  final case class InitialSessionNotificationsStartingTomorrow(
    initialDelay: FiniteDuration,
    interval: FiniteDuration
  )

  final case class ScheduleFeedbackEmailsStartingToday(
    eventualSessions: Future[List[GetSessionByIdResponse]]
  )

  final case class InitiateFeedbackEmailsStartingTomorrow(
    initialDelay: FiniteDuration,
    interval: FiniteDuration
  )

}

class SessionScheduler(
  schedulerService: SchedulerService,
  userService: UserManagementService,
  feedbackFormResponseService: FeedbackFormResponseService,
  conf: Config,
  mailerService: MailerService
)(implicit logger: LoggingAdapter)
    extends Actor {

  val dateTimeUtility = new DateTimeUtility
  val zeroDuration: FiniteDuration = Duration.Zero
  val intervalPeriod: FiniteDuration = conf.getConfig("scheduler").getInt("interval").day
  val frontendURL: String = conf.getConfig("frontendURL").getString("ui-url")

  def scheduler: Scheduler = context.system.scheduler

  override def preStart(): Unit = {
    val millis = dateTimeUtility.nowMillis
    val initialDelay = ((dateTimeUtility.endOfDayMillis + 61 * 1000) - millis).milliseconds

    val tenHrsDelay: LocalDateTime =
      dateTimeUtility.toLocalDateTime(dateTimeUtility.endOfDayMillis - millis).plusHours(10)
    val tenHrsDelayMillis = dateTimeUtility.toMillis(tenHrsDelay).milliseconds
    self ! ScheduleFeedbackRemindersStartingToday(sessionsForToday(ExpiringNextNotReminded))
    self ! InitialFeedbackRemindersStartingTomorrow(tenHrsDelayMillis, intervalPeriod)

    self ! ScheduleSessionNotificationsStartingToday(
      sessionsForToday(SchedulingNextUnNotified)
    )
    self ! InitialSessionNotificationsStartingTomorrow(tenHrsDelayMillis, intervalPeriod)
    self ! ScheduleFeedbackEmailsStartingToday(sessionsForToday(SchedulingNext))
    self ! InitiateFeedbackEmailsStartingTomorrow(initialDelay, intervalPeriod)
  }

  def receive: Receive = {
    case ScheduleFeedbackRemindersStartingToday(expiringSessions) =>
      scheduleEmails(expiringSessions, Reminder)

    case InitialFeedbackRemindersStartingTomorrow(initialDelay, interval) =>
      scheduler.scheduleAtFixedRate(
        initialDelay = initialDelay,
        interval = interval,
        receiver = self,
        message = ScheduleFeedbackRemindersStartingTomorrow
      )
      val eventualSessions = sessionsForToday(ExpiringNextNotReminded)
      scheduleEmails(eventualSessions, Reminder)

    case ScheduleFeedbackRemindersStartingTomorrow =>
      val eventualSessions = sessionsForToday(ExpiringNextNotReminded)
      scheduleEmails(eventualSessions, Reminder)

    case ScheduleSessionNotificationsStartingToday(eventualSessions) =>
      scheduleEmails(eventualSessions, Notification)

    case InitialSessionNotificationsStartingTomorrow(initialDelay, interval) =>
      scheduler.scheduleAtFixedRate(
        initialDelay = initialDelay,
        interval = interval,
        receiver = self,
        message = ScheduleSessionNotificationStartingTomorrow
      )

    case ScheduleSessionNotificationStartingTomorrow =>
      val eventualSessions = sessionsForToday(SchedulingNextUnNotified)
      scheduleEmails(eventualSessions, Notification)

    case ScheduleFeedbackEmailsStartingToday(eventualSessions) =>
      scheduleEmails(eventualSessions, Feedback)

    case InitiateFeedbackEmailsStartingTomorrow(initialDelay, interval) =>
      scheduler.scheduleAtFixedRate(
        initialDelay = initialDelay,
        interval = interval,
        receiver = self,
        message = ScheduleFeedbackEmailsStartingTomorrow
      )

    case ScheduleFeedbackEmailsStartingTomorrow =>
      val eventualSessions = sessionsForToday(SchedulingNext)
      scheduleEmails(eventualSessions, Feedback)

    case RefreshSessionsSchedulers =>
      val eventualSessions = sessionsForToday(SchedulingNext)
      val _ = scheduleEmails(eventualSessions, Feedback)

      val eventualNotifications =
        sessionsForToday(SchedulingNextUnNotified)
      scheduleEmails(eventualNotifications, Notification)

    case ScheduleSession(sessionId) =>
      val eventualSession = schedulerService.getSessionById(sessionId).map {
        case Right(sessions) => List(sessions)
        case Left(_) => List()
      }
      scheduleEmails(eventualSession, Feedback)

    case SendMail(sessions, emailType) if sessions.nonEmpty =>
      val recipients = userService.getActiveAndUnBannedEmails.map {
        case Right(emails) => emails
        case Left(_) => List()
      }
      val emailInfo = sessions.map(session => EmailInfo(session.topic, session.email, session.date.toString))
      recipients collect {
        case emails if emails.nonEmpty =>
          emailType match {
            case Reminder => reminderEmailHandler(sessions, emailInfo, emails)
            case Feedback => feedbackEmailHandler(sessions, emailInfo, emails)
            case Notification =>
              notificationEmailHandler(sessions, emailInfo, emails)
          }
      }
  }

  def scheduleEmails(
    eventualSessions: Future[List[GetSessionByIdResponse]],
    emailType: EmailType
  ): Future[List[Cancellable]] =
    eventualSessions.map {
      case sessions if sessions.nonEmpty =>
        emailType match {
          case Reminder =>
            List(scheduler.scheduleOnce(zeroDuration, self, SendMail(sessions, Reminder)))

          case Feedback =>
            sessions.map { session =>
              val delay = (session.date.getTime - dateTimeUtility.nowMillis).milliseconds
              scheduler.scheduleOnce(
                delay,
                self,
                SendMail(List(session), Feedback)
              )
            }
          case Notification =>
            List(
              scheduler.scheduleOnce(
                zeroDuration,
                self,
                SendMail(sessions, Notification)
              )
            )
        }
    }

  def reminderEmailHandler(
    sessions: List[GetSessionByIdResponse],
    sessionsEmailInfo: List[EmailInfo],
    emails: List[String]
  ): Unit = {

    val emailsWithNoResponseSessions =
      sessions map { session =>
        val responseEmails = feedbackFormResponseService.getAllResponseEmailsPerSession(session.id).map {
          case Right(emails) => emails
          case Left(_) => List()
        }
        responseEmails.map { responseEmails =>
          val defaulterEmails = emails diff responseEmails diff List(
                  session.email
                )
          defaulterEmails map (defaulter =>
            (
              defaulter,
              sessionsEmailInfo.filter(_.presenter == session.email).head
            )
          )

        }
      }

    val eventualEmailsWithNoResponseSessions =
      Future.sequence(emailsWithNoResponseSessions).map(_.flatten)

    val eventualEmailsMappedNoResponseSessions =
      eventualEmailsWithNoResponseSessions.map { emailsWithNoResponseSessions =>
        emailsWithNoResponseSessions.groupBy { case (defaulterEmail, _) => defaulterEmail }.map {
          case (email, sessionEmailInfo) =>
            (
              email,
              sessionEmailInfo.map {
                case (_, emailInfo) => emailInfo
              }
            )
        }
      }

    eventualEmailsMappedNoResponseSessions.map { emailsMappedNoResponseSessions =>
      emailsMappedNoResponseSessions.foreach {
        case (email, sessionEmailInfo) =>
          val result = mailerService.sendMessage(
            List(email),
            "Feedback reminder",
            EmailContent.setReminderMailContent(s"$frontendURL/feedbackform/response", sessionEmailInfo)
          )
          result.andThen {
            case Failure(exception) => logger.error(exception.toString)
          }
          result.recover {
            case _ => List(akka.Done)
          }
      }
    }
    sessions.map { session =>
      val emailType = "Reminder"
      schedulerService.updateMailStatus(session.id, emailType)
    }
  }

  def notificationEmailHandler(
    sessions: List[GetSessionByIdResponse],
    emailsInfo: List[EmailInfo],
    emails: List[String]
  ): Unit =
    sessions.map { session =>
      val emailType = "Notification"
      schedulerService.updateMailStatus(session.id, emailType).map {
        case true =>
          val result = mailerService.sendMessage(
            emails,
            "Knolx/Meetup Sessions",
            EmailContent.setNotificationMailContent(emailsInfo)
          )
          result.andThen {
            case Failure(exception) => logger.error(exception.toString)
          }
          result.recover {
            case _ => List(akka.Done)
          }
        case _ => ()

      }
    }

  def feedbackEmailHandler(
    sessions: List[GetSessionByIdResponse],
    emailInfo: List[EmailInfo],
    emails: List[String]
  ): Unit = {
    val emailsExceptPresenter = emails.filterNot(_.equals(sessions.head.email))
    val result = mailerService.sendMessage(
      emailsExceptPresenter,
      s"${sessions.head.topic} Feedback Form",
      EmailContent.setFeedbackMailContent(emailInfo, s"$frontendURL/feedbackform/response")
    )
    result.andThen {
      case Failure(exception) => logger.error(exception.toString)
    }
    result.recover {
      case _ => List(akka.Done)
    }
  }

  def sessionsForToday(sessionState: SessionState): Future[List[GetSessionByIdResponse]] =
    schedulerService.sessionsForToday(sessionState).map {
      case Right(sessions) => sessions
      case Left(_) => List()
    }
}
