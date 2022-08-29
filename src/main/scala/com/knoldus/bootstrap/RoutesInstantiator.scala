package com.knoldus.bootstrap

import akka.event.LoggingAdapter
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.server.Directives.{ concat, ignoreTrailingSlash }
import akka.http.scaladsl.server.Route
import com.knoldus.HttpServerApp.errorManagement
import com.knoldus.bootstrap.CORSSupport._
import com.knoldus.domain.user.UserToken
import com.knoldus.routes.BaseRoutes
import com.knoldus.routes.feedbackform.{ FeedbackFormReportRoutes, FeedbackFormResponseRoutes, FeedbackFormRoutes }
import com.knoldus.routes.info.InfoRoutes
import com.knoldus.routes.knolxanalysis.KnolxAnalysisRoutes
import com.knoldus.routes.recommendation.RecommendationRoutes
import com.knoldus.routes.session.{ CalendarRoutes, CategoryRoutes, MeetingRoutes, NewSessionRoutes }
import com.knoldus.routes.slot.SlotRoutes
import com.knoldus.routes.statistic.StatisticalRoutes
import com.knoldus.routes.user.KnolxUserAnalysisRoutes
import com.knoldus.routes.usermanagement.{ NewUserRoutes, UserManagementRoutes }
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$
class RoutesInstantiator(
  conf: Config,
  services: ServiceInstantiator,
  cache: Cache[String, UserToken]
)(implicit
  val ec: ExecutionContext,
  logger: LoggingAdapter
) extends PlayJsonSupport {
  private val infoRoutes = new InfoRoutes(services.infoService)

  private val newSessionRoutes = new NewSessionRoutes(services.newSessionService, services.authorizationService)

  private val userManagementRoutes =
    new UserManagementRoutes(services.userManagementService, cache, services.authorizationService)

  private val recommendationRoutes =
    new RecommendationRoutes(services.recommendationService, services.authorizationService)
  private val feedbackRoutes = new FeedbackFormRoutes(services.feedbackFormService, services.authorizationService)
  private val slotRoutes = new SlotRoutes(services.slotService, services.authorizationService)

  private val feedbackFormResponseRoutes =
    new FeedbackFormResponseRoutes(services.feedbackFormResponseService, services.authorizationService)
  private val categoryRoutes = new CategoryRoutes(services.categoryService, services.authorizationService)
  private val newUserRoutes = new NewUserRoutes(services.newUserService, services.authorizationService)

  private val feedbackFormReportRoutes =
    new FeedbackFormReportRoutes(services.feedbackFormReportService, services.authorizationService)

  private val knolxUserAnalysisRoutes =
    new KnolxUserAnalysisRoutes(services.knolxUserAnalysisService, services.authorizationService)
  private val calendarRoutes = new CalendarRoutes(services.calenderService, services.authorizationService)

  private val knolxAnalysisRoutes =
    new KnolxAnalysisRoutes(services.knolxAnalysisService)
  private val statisticalRoutes = new StatisticalRoutes(services.statisticalService, services.authorizationService)

  private val meetingRoutes = new MeetingRoutes(services.meetingService, services.authorizationService)

  val routes: Route = handleErrors {
    handleCORS {
      BaseRoutes.seal(conf) {
        ignoreTrailingSlash {
          concat(
            infoRoutes.routes,
            recommendationRoutes.routes,
            feedbackRoutes.routes,
            feedbackFormResponseRoutes.routes,
            categoryRoutes.routes,
            feedbackFormReportRoutes.routes,
            knolxUserAnalysisRoutes.routes,
            calendarRoutes.routes,
            knolxAnalysisRoutes.routes,
            userManagementRoutes.routes,
            statisticalRoutes.routes,
            newSessionRoutes.routes,
            slotRoutes.routes,
            newUserRoutes.routes,
            meetingRoutes.routes
          )
        }
      }
    }
  }
}

// $COVERAGE-ON$
