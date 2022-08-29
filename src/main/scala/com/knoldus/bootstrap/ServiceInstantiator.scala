package com.knoldus.bootstrap

import akka.actor.{ ActorRef, ActorSystem, Props, Scheduler }
import akka.event.LoggingAdapter
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.{ Http, HttpExt }
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.services.directory.Directory
import com.knoldus.domain.user.UserToken
import com.knoldus.services.IAM.IAMService
import com.knoldus.services.email.{ MailerService, NotificationService }
import com.knoldus.services.feedbackform.{ FeedbackFormReportService, FeedbackFormResponseService, FeedbackFormService }
import com.knoldus.services.info.InfoService
import com.knoldus.services.keyCloak.{ KeycloakIAMService, KeycloakInstance }
import com.knoldus.services.knolxanalysis.KnolxAnalysisService
import com.knoldus.services.recommendation.RecommendationService
import com.knoldus.services.rollbarservice.RollbarService
import com.knoldus.services.scheduler._
import com.knoldus.services.session.{
  CalendarService,
  CategoryService,
  MeetingService,
  NewSessionService,
  SessionService
}
import com.knoldus.services.slot.SlotService
import com.knoldus.services.statistic.StatisticalService
import com.knoldus.services.user.KnolxUserAnalysisService
import com.knoldus.services.usermanagement.utilities.PasswordUtility
import com.knoldus.services.usermanagement.{ AuthorizationService, NewUserService, UserManagementService }
import com.knoldus.utils.GoogleAdminSDKCredentials
import com.knoldus.utils.GoogleAdminSDKCredentials.JSON_FACTORY
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$
class ServiceInstantiator(
  conf: Config,
  daoInstantiator: DaoInstantiator,
  cache: Cache[String, UserToken],
  clientSecretCache: Cache[String, String],
  actorSystem: ActorSystem
)(implicit
  system: ActorSystem,
  val logger: LoggingAdapter
) {

  implicit val ec: ExecutionContext = system.dispatcher
  implicit val scheduler: Scheduler = system.scheduler

  private lazy val http: HttpExt = Http()

  val iamService: IAMService = new KeycloakIAMService(keycloakInstance, conf)
  lazy val passwordUtility = new PasswordUtility
  lazy val mailerService: MailerService = new NotificationService(conf = conf, http = http)
  lazy val infoService: InfoService = new InfoService(conf, daoInstantiator)
  lazy val keycloakInstance: KeycloakInstance = new KeycloakInstance(conf)
  lazy val newUserService: NewUserService = new NewUserService(daoInstantiator.userDao, conf, iamService)

  lazy val authorizationService =
    new AuthorizationService(cache, clientSecretCache, daoInstantiator.userDao, iamService)
  implicit lazy val errorManagementService: RollbarService = new RollbarService(conf)

  lazy val userManagementService: UserManagementService =
    new UserManagementService(
      daoInstantiator.userDao,
      passwordUtility,
      conf,
      mailerService
    )
  val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport

  val service: Directory = new Directory.Builder(
    HTTP_TRANSPORT,
    JSON_FACTORY,
    GoogleAdminSDKCredentials.getCredentials(HTTP_TRANSPORT)
  ).setApplicationName("Ticket Service Client").build

  lazy val newSessionService: NewSessionService =
    new NewSessionService(daoInstantiator.slotDao, daoInstantiator.newSessionDao, service)

  lazy val recommendationService: RecommendationService =
    new RecommendationService(
      daoInstantiator.recommendationDao,
      daoInstantiator.recommendationsResponseDao,
      daoInstantiator.userDao,
      mailerService
    )

  lazy val feedbackFormService: FeedbackFormService = new FeedbackFormService(
    daoInstantiator.feedbackFormDao,
    daoInstantiator.sessionDao
  )

  lazy val feedbackFormResponseService: FeedbackFormResponseService = new FeedbackFormResponseService(
    conf,
    mailerService,
    daoInstantiator.feedbackFormResponseDao,
    daoInstantiator.feedbackFormDao,
    daoInstantiator.sessionDao,
    daoInstantiator.userDao
  )

  lazy val schedulerService = new SchedulerService(
    daoInstantiator.sessionDao,
    daoInstantiator.feedbackFormResponseDao,
    daoInstantiator.userDao
  )

  val monthlyReport: ActorRef = actorSystem.actorOf(
    Props(
      new MonthlySessionReportScheduler(
        schedulerService,
        recommendationService,
        conf,
        mailerService
      )
    )
  )

  val userBanReportScheduler: ActorRef = actorSystem.actorOf(
    Props(
      new UserBanReportScheduler(
        schedulerService,
        conf,
        mailerService
      )
    )
  )

  lazy val sessionSchedulerActor: ActorRef = actorSystem.actorOf(
    Props(
      new SessionScheduler(schedulerService, userManagementService, feedbackFormResponseService, conf, mailerService)
    )
  )

  locally {
    val _ = actorSystem.actorOf(
      Props(
        new UsersBanScheduler(schedulerService, userManagementService, feedbackFormResponseService, conf, mailerService)
      )
    )
  }

  lazy val slotService: SlotService = new SlotService(
    conf,
    daoInstantiator.slotDao,
    daoInstantiator.newSessionDao,
    daoInstantiator.holidayDao
  )

  lazy val categoryService: CategoryService = new CategoryService(
    daoInstantiator.categoryDao,
    daoInstantiator.sessionDao
  )

  lazy val feedbackFormReportService: FeedbackFormReportService = new FeedbackFormReportService(
    daoInstantiator.feedbackFormResponseDao,
    daoInstantiator.sessionDao,
    daoInstantiator.userDao
  )

  lazy val knolxUserAnalysisService: KnolxUserAnalysisService = new KnolxUserAnalysisService(
    daoInstantiator.feedbackFormResponseDao,
    daoInstantiator.sessionDao,
    daoInstantiator.userDao
  )

  lazy val calenderService: CalendarService =
    new CalendarService(daoInstantiator.calendarDao, daoInstantiator.userDao, mailerService)

  lazy val knolxAnalysisService: KnolxAnalysisService = new KnolxAnalysisService(
    daoInstantiator.sessionDao,
    daoInstantiator.categoryDao
  )

  lazy val statisticalService = new StatisticalService(daoInstantiator.sessionDao, daoInstantiator.userDao)

  lazy val meetingService = new MeetingService(daoInstantiator.newSessionDao, conf)

}
// $COVERAGE-ON$
