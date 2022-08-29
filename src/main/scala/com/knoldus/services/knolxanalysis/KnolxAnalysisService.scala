package com.knoldus.services.knolxanalysis

import java.util.Date
import com.knoldus.dao.filters.TrueFilter
import com.knoldus.dao.session.SessionDao.{ Active, Cancelled }
import com.knoldus.routes.contract.knolxanalysis.{ CategoryInformation, KnolxMonthlyInfo, SubCategoryInformation }
import cats.implicits._
import akka.event.LoggingAdapter
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.{ CategoryDao, SessionDao }
import com.knoldus.dao.session.SessionDao.{ EndDate, StartDate }
import com.knoldus.domain.session.Session
import com.knoldus.routes.contract.session.GetSessionByIdResponse
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.knolxanalysis.KnolxAnalysisService.KnolxAnalysisServiceError

import scala.concurrent.{ ExecutionContext, Future }

class KnolxAnalysisService(sessionDao: SessionDao, categoryDao: CategoryDao)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {
  val dateTimeUtility = new DateTimeUtility

  private def getSessionsInTimeRange(startDate: String, endDate: String): Future[Seq[WithId[Session]]] = {
    val startDateLong = dateTimeUtility.parseDateStringToIST(startDate)
    val endDateLong = dateTimeUtility.parseDateStringToIST(endDate)
    val seqOfSessions = sessionDao.listAll(
      And(
        And(StartDate(new Date(startDateLong)), EndDate(new Date(endDateLong))),
        And(Active(true), Cancelled(false))
      )
    )
    seqOfSessions
  }

  def getSessionInRange(
    startDate: String,
    endDate: String
  ): Future[Either[KnolxAnalysisServiceError, Seq[WithId[Session]]]] = {

    val sessions = getSessionsInTimeRange(startDate, endDate)
    sessions.map { seqOfSessions =>
      seqOfSessions.asRight
    }
  }

  def doCategoryAnalysis(
    startDate: String,
    endDate: String
  ): Future[Either[KnolxAnalysisServiceError, (List[CategoryInformation], Int)]] =
    categoryDao.listAll(TrueFilter).flatMap { categoryInfo =>
      val primaryCategorySeq = categoryInfo.map(_.entity.categoryName)
      val sessions = getSessionsInTimeRange(startDate, endDate)
      sessions.flatMap { seqOfSessions =>
        val categoryUsedInSession = seqOfSessions.groupBy(_.entity.category).keys.toList
        val categoryNotUsedInSession = primaryCategorySeq diff categoryUsedInSession
        val categoriesUsedAnalysisInfo = seqOfSessions
          .groupBy(_.entity.category)
          .map {
            case (categoryName, categoryBasedSession) =>
              val subCategoryInfo: List[SubCategoryInformation] =
                categoryBasedSession
                  .groupBy(_.entity.subCategory)
                  .map {
                    case (
                          subCategoryName,
                          sessionBasedSubcategory
                        ) =>
                      SubCategoryInformation(
                        subCategoryName,
                        sessionBasedSubcategory.length
                      )
                  }
                  .toList
              CategoryInformation(
                categoryName,
                categoryBasedSession.length,
                subCategoryInfo
              )
          }
          .toList
        val categoriesAnalysisInfo = categoriesUsedAnalysisInfo ::: categoryNotUsedInSession.toList
                .map(category => CategoryInformation(category, 0, Nil))
        val result = Tuple2(categoriesAnalysisInfo, seqOfSessions.length)
        Future.successful(result.asRight)
      }
    }

  def doKnolxMonthlyAnalysis(
    startDate: String,
    endDate: String
  ): Future[Either[KnolxAnalysisServiceError, List[KnolxMonthlyInfo]]] = {

    val sessions = getSessionsInTimeRange(startDate, endDate)
    val result = sessions.map { sessionInfo =>
      sessionInfo
        .groupBy(session => dateTimeUtility.yearMonthFormat(session.entity.date))
        .map {
          case (date, dateBasedSessions) =>
            KnolxMonthlyInfo(date, dateBasedSessions.size)
        }
        .toList
    }
    result.map { listOfMonthlyKnolx =>
      listOfMonthlyKnolx.asRight
    }
  }

  def sessionsInTimeRange(
    email: Option[String],
    startDate: Date,
    endDate: Date
  ): Future[Either[KnolxAnalysisServiceError, List[GetSessionByIdResponse]]] = {
    val filter1 = And(Active(true), Cancelled(false))
    val filter2 = And(StartDate(startDate), EndDate(endDate))
    val filter = And(filter1, filter2)
    sessionDao.listAll(filter).map { data =>
      if (data.nonEmpty)
        data
          .map { sessionInfo =>
            val fullName = sessionInfo.entity.email
              .split("@")
              .headOption
              .fold("Invalid") { name =>
                name.split('.').map(_.capitalize).mkString(" ")
              }
              .trim
            email match {
              case Some(email) =>
                val sessionInformation = GetSessionByIdResponse(
                  sessionInfo.entity.userId,
                  email,
                  fullName,
                  sessionInfo.entity.date,
                  sessionInfo.entity.session,
                  sessionInfo.entity.category,
                  sessionInfo.entity.subCategory,
                  sessionInfo.entity.feedbackFormId,
                  sessionInfo.entity.topic,
                  sessionInfo.entity.brief,
                  sessionInfo.entity.feedbackExpirationDays,
                  sessionInfo.entity.meetup,
                  sessionInfo.entity.rating,
                  sessionInfo.entity.score,
                  sessionInfo.entity.cancelled,
                  sessionInfo.entity.active,
                  sessionInfo.entity.expirationDate,
                  sessionInfo.entity.youtubeURL.fold("")(identity),
                  sessionInfo.entity.slideShareURL.fold("")(identity),
                  sessionInfo.entity.temporaryYoutubeURL,
                  sessionInfo.entity.reminder,
                  sessionInfo.entity.notification,
                  sessionInfo.id
                )
                sessionInformation

              case None =>
                val sessionInformation = GetSessionByIdResponse(
                  sessionInfo.entity.userId,
                  sessionInfo.entity.email,
                  fullName,
                  sessionInfo.entity.date,
                  sessionInfo.entity.session,
                  sessionInfo.entity.category,
                  sessionInfo.entity.subCategory,
                  sessionInfo.entity.feedbackFormId,
                  sessionInfo.entity.topic,
                  sessionInfo.entity.brief,
                  sessionInfo.entity.feedbackExpirationDays,
                  sessionInfo.entity.meetup,
                  sessionInfo.entity.rating,
                  sessionInfo.entity.score,
                  sessionInfo.entity.cancelled,
                  sessionInfo.entity.active,
                  sessionInfo.entity.expirationDate,
                  sessionInfo.entity.youtubeURL.fold("")(identity),
                  sessionInfo.entity.slideShareURL.fold("")(identity),
                  sessionInfo.entity.temporaryYoutubeURL,
                  sessionInfo.entity.reminder,
                  sessionInfo.entity.notification,
                  sessionInfo.id
                )
                sessionInformation
            }
          }
          .toList
          .asRight
      else
        KnolxAnalysisServiceError.SessionsNotFoundError.asLeft
    }
  }
}

object KnolxAnalysisService {

  sealed trait KnolxAnalysisServiceError

  object KnolxAnalysisServiceError {
    case object SessionsNotFoundError extends KnolxAnalysisServiceError
  }
}
