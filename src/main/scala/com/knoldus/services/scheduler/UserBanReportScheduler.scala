package com.knoldus.services.scheduler

import akka.Done
import akka.actor.{ Actor, Scheduler }
import akka.event.LoggingAdapter
import com.knoldus.domain.user.UserInformation
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.typesafe.config.Config

import java.util.Calendar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure

final case class InitiateBanReportEmails(initialDelay: FiniteDuration, interval: FiniteDuration)
case object ScheduleBanReportEmails

class UserBanReportScheduler(schedulerService: SchedulerService, conf: Config, mailerService: MailerService)(implicit
  logger: LoggingAdapter
) extends Actor {
  val dateTimeUtility = new DateTimeUtility
  val intervalPeriod: Int = conf.getConfig("scheduler").getInt("interval")
  val monthlyReportsEnabled: Boolean = conf.getBoolean("knolx-settings.monthly-reports-enabled")

  override def preStart(): Unit =
    if (monthlyReportsEnabled) {
      val initialDelayForBanReportEmails =
        dateTimeUtility
          .toMillis(
            dateTimeUtility
              .toLocalDateTime(
                dateTimeUtility.endOfDayMillis - dateTimeUtility.nowMillis
              )
              .plusHours(10)
          )
          .milliseconds
      self ! InitiateBanReportEmails(initialDelayForBanReportEmails, intervalPeriod.day)
    }

  def receive: Receive = {
    case InitiateBanReportEmails(initialDelay, interval) =>
      scheduler.scheduleAtFixedRate(
        initialDelay,
        interval = interval,
        receiver = self,
        message = ScheduleBanReportEmails
      )

    case ScheduleBanReportEmails =>
      val currentDate = Calendar.getInstance().get(Calendar.DATE)
      if (currentDate == 1) scheduleBanReportEmails()
  }

  def scheduler: Scheduler = context.system.scheduler

  def scheduleBanReportEmails(): Future[Done] = {
    def sendMails(
      currentBannedUsers: Seq[UserInformation],
      allCoreMembers: Seq[UserInformation],
      allBannedUsersOfLastMonth: Seq[UserInformation]
    ): Future[Done] = {
      val receivers = allCoreMembers.map(_.email)
      val result = mailerService.sendMessage(
        receivers.toList,
        "Monthly Ban Users Report",
        EmailContent.setContentForBanUserReport(currentBannedUsers.toList, allBannedUsersOfLastMonth.toList)
      )
      result.andThen {
        case Failure(exception) => logger.error(exception.toString)
      }
      result.recover {
        case _ => akka.Done
      }
    }

    for {
      allBannedUsersOfLastMonth <- schedulerService.getAllBannedUsersOfLastMonth
      allCoreMembers <- schedulerService.getAllCoreMembers
      currentBannedUsers <- schedulerService.getAllBannedUsers
      result <- sendMails(currentBannedUsers, allCoreMembers, allBannedUsersOfLastMonth)
    } yield result
  }
}
