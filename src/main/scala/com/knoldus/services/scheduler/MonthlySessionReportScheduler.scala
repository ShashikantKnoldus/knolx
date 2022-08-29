package com.knoldus.services.scheduler

import akka.Done
import akka.actor.{ Actor, Scheduler }
import akka.event.LoggingAdapter
import com.knoldus.domain.session.SessionUnattendedUserInfo
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.recommendation.RecommendationService
import com.knoldus.services.scheduler.MonthlySessionReportScheduler.{
  InitiateMonthlyReportOfSessionNotAttendedUsers,
  ScheduleMonthlyReportOfSessionNotAttendedUsers
}
import com.typesafe.config.Config

import java.util.Calendar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{ FiniteDuration, _ }
import scala.util.Failure

object MonthlySessionReportScheduler {

  final case class InitiateMonthlyReportOfSessionNotAttendedUsers(delay: FiniteDuration, interval: FiniteDuration)

  case object ScheduleMonthlyReportOfSessionNotAttendedUsers

}

class MonthlySessionReportScheduler(
  schedulerService: SchedulerService,
  recommendationService: RecommendationService,
  conf: Config,
  mailerService: MailerService
)(implicit logger: LoggingAdapter)
    extends Actor {

  val dateTimeUtility = new DateTimeUtility
  val intervalPeriod: Int = conf.getConfig("scheduler").getInt("interval")
  val monthlyReportsEnabled: Boolean = conf.getBoolean("knolx-settings.monthly-reports-enabled")

  override def preStart(): Unit =
    if (monthlyReportsEnabled) {
      val initialDelayForMonthlyReportOfNotAttendedUsers =
        dateTimeUtility
          .toMillis(
            dateTimeUtility
              .toLocalDateTime(
                dateTimeUtility.endOfDayMillis - dateTimeUtility.nowMillis
              )
              .plusHours(10)
          )
          .milliseconds
      self ! InitiateMonthlyReportOfSessionNotAttendedUsers(
        initialDelayForMonthlyReportOfNotAttendedUsers,
        intervalPeriod.day
      )
    }

  def receive: Receive =
    initializingHandler orElse
        schedulingHandler

  def initializingHandler: Receive = {
    case InitiateMonthlyReportOfSessionNotAttendedUsers(delay, interval) =>
      scheduler.scheduleAtFixedRate(
        initialDelay = delay,
        interval = interval,
        receiver = self,
        message = ScheduleMonthlyReportOfSessionNotAttendedUsers
      )(context.dispatcher)

  }

  def scheduler: Scheduler = context.system.scheduler

  def schedulingHandler: Receive = {
    case ScheduleMonthlyReportOfSessionNotAttendedUsers =>
      val currentDate = Calendar.getInstance().get(Calendar.DATE)
      if (currentDate == 1) scheduleMonthlyReportEmails
  }

  def scheduleMonthlyReportEmails: Future[Done] = {
    val sessionWithNotAttendedUsers: Future[List[SessionUnattendedUserInfo]] =
      schedulerService.getUsersNotAttendedLastMonthSession
    sessionWithNotAttendedUsers.flatMap { users =>
      val receivers = recommendationService.getAllAdminAndSuperUser.map { emails =>
        emails
      }
      receivers.flatMap { emails =>
        val result = mailerService.sendMessage(
          emails,
          "Monthly Report of users not attended session",
          EmailContent.setContentForMonthlyReportOfUsersNotAttendedSession(users)
        )
        result.andThen {
          case Failure(exception) => logger.error(exception.toString)
        }
        result.recover {
          case _ => akka.Done
        }
      }
    }
  }

}
