package com.knoldus.services.scheduler

import akka.actor.{ Actor, Cancellable, Scheduler }
import com.knoldus.routes.contract.session.GetSessionByIdResponse
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.feedbackform.FeedbackFormResponseService
import com.knoldus.services.scheduler.SessionState.ExpiringNext
import com.knoldus.services.scheduler.UsersBanScheduler._
import com.knoldus.services.usermanagement.UserManagementService
import com.typesafe.config.Config
import java.time.LocalDateTime

import akka.event.LoggingAdapter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{ FiniteDuration, _ }
import scala.util.Failure

object UsersBanScheduler {

  final case class EmailBodyInfo(topic: String, presenter: String, date: String)

  final case class BanEmailInformation(to: String, body: List[EmailBodyInfo])

  case object ScheduleBanEmails

  final case class InitiateBanEmails(initialDelay: FiniteDuration, interval: FiniteDuration)

  final case class SendMail(session: BanEmailInformation)

}

class UsersBanScheduler(
  schedulerService: SchedulerService,
  userService: UserManagementService,
  feedbackFormResponseService: FeedbackFormResponseService,
  conf: Config,
  mailerService: MailerService
)(implicit logger: LoggingAdapter)
    extends Actor {

  val dateTimeUtility = new DateTimeUtility

  val intervalPeriod: Int = conf.getConfig("scheduler").getInt("interval")

  val banEnabled: Boolean = conf.getBoolean("knolx-settings.ban-enabled")

  override def preStart(): Unit =
    if (banEnabled) {
      val executionDelay: LocalDateTime = dateTimeUtility
        .toLocalDateTime(
          dateTimeUtility.endOfDayMillis - dateTimeUtility.nowMillis
        )
        .minusMinutes(30)
      val initialDelay: FiniteDuration =
        dateTimeUtility.toMillis(executionDelay).milliseconds

      self ! InitiateBanEmails(initialDelay, intervalPeriod.day)
    }

  def scheduler: Scheduler = context.system.scheduler

  def receive: Receive =
    initializingHandler orElse
        schedulingHandler orElse
        emailHandler

  def initializingHandler: Receive = {
    case InitiateBanEmails(initialDelay, interval) =>
      scheduler.scheduleAtFixedRate(
        initialDelay = initialDelay,
        interval = interval,
        receiver = self,
        message = ScheduleBanEmails
      )(context.dispatcher)
  }

  def schedulingHandler: Receive = {
    case ScheduleBanEmails =>
      val eventuallyExpiringSession = sessionsExpiringToday
      scheduleBanEmails(
        eventuallyExpiringSession
      )
  }

  def emailHandler: Receive = {
    case SendMail(banEmailInformation) =>
      userService.banUser(banEmailInformation.to).map { response =>
        if (response) {
          val result = mailerService.sendMessage(
            List(banEmailInformation.to),
            "BAN FROM KNOLX",
            EmailContent.setContentForBanUser(banEmailInformation.body)
          )
          result.andThen {
            case Failure(exception) => logger.error(exception.toString)
          }
          result.recover {
            case _ => List(akka.Done)
          }
        }
      }
  }

  def getInfoToBan(
    sessions: List[GetSessionByIdResponse],
    allUnbanEmails: List[String]
  ): Future[List[BanEmailInformation]] =
    Future
      .sequence(sessions.map { session =>
        feedbackFormResponseService
          .getAllResponseEmailsPerSession(session.id)
          .map {
            case Right(emails) => emails
            case Left(_) => List()
          }
          .map { responseEmails =>
            val emailsToBan = allUnbanEmails.filter(_ != session.email) diff responseEmails
            Map(
              EmailBodyInfo(session.topic, session.email, session.date.toString) -> emailsToBan
            )
          }
      })
      .map { banned =>
        val banInfo = banned.flatten.toMap
        val bannedEmails = banInfo.values.toList.flatten.distinct
        bannedEmails map { email =>
          val emailBodyInfo = banInfo.keys.collect {
            case sessionTopic if banInfo(sessionTopic).contains(email) =>
              sessionTopic
          }.toList
          BanEmailInformation(email, emailBodyInfo)
        }
      }

  def scheduleBanEmails(
    eventualExpiringSessions: Future[List[GetSessionByIdResponse]]
  ): Future[Map[String, Cancellable]] =
    eventualExpiringSessions.flatMap { sessions =>
      val recipients = userService.getActiveAndUnBannedEmails.map {
        case Right(emails) => emails
        case Left(_) => List()
      }
      recipients.flatMap { emails =>
        getInfoToBan(sessions, emails).map {
          _.map { emailContent =>
            emailContent.to -> scheduler.scheduleOnce(
              Duration.Zero,
              self,
              SendMail(emailContent)
            )
          }.toMap
        }
      }
    }

  def sessionsExpiringToday: Future[List[GetSessionByIdResponse]] =
    schedulerService.sessionsForToday(ExpiringNext).map {
      case Right(sessions) => sessions
      case Left(_) => List()
    }
}
