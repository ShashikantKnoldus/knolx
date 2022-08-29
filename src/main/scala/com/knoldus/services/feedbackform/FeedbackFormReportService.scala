package com.knoldus.services.feedbackform

import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao.StoredSessionId
import com.knoldus.dao.filters.{ And, Or }
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.{ Active, DateMatch, UserKnolxSession }
import com.knoldus.dao.sorting.Direction.Descending
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.feedbackform.UserFeedbackResponse
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.feedbackform.{
  FeedbackReport,
  FeedbackReportHeader,
  ReportResult,
  UserFeedbackReport
}
import com.knoldus.services.feedbackform.FeedbackFormReportService.FeedbackFormReportServiceError

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class FeedbackFormReportService(
  feedbackFormsResponseDao: FeedbackFormsResponseDao,
  sessionDao: SessionDao,
  userDao: UserDao
)(
  implicit val ec: ExecutionContext,
  implicit val logger: LoggingAdapter
) {

  def manageAllFeedbackReports(
    pageNumber: Int
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[FeedbackFormReportServiceError, ReportResult]] = {
    val currentDate = new Date()
    val sessionsTillNow = sessionDao.list(
      And(Or(Active(true), Active(false)), SessionDao.SessionDate(currentDate)),
      pageNumber,
      8,
      Some(SortBy((DateMatch, Descending)))
    )
    if (authorityUser.keycloakRole == Admin)
      generateReport(sessionsTillNow).flatMap { result =>
        sessionDao.count(Or(Active(true), Active(false))).map { count =>
          val pages = Math.ceil(count.toDouble / 8).toInt
          ReportResult(result, pageNumber, pages).asRight
        }
      }
    else
      Future.successful(FeedbackFormReportServiceError.FeedbackAccessDeniedError.asLeft)
  }

  def manageUserFeedbackReports(
    userId: String,
    pageNumber: Int
  ): Future[Either[FeedbackFormReportServiceError, ReportResult]] = {
    val sessionsTillNow = sessionDao.list(UserKnolxSession(userId), pageNumber, 8)
    generateReport(sessionsTillNow).flatMap { result =>
      sessionDao.count(And(Or(Active(true), Active(false)), UserKnolxSession(userId))).map { count =>
        val pages = Math.ceil(count.toDouble / 8).toInt
        ReportResult(result, pageNumber, pages).asRight
      }
    }
  }

  private def generateReport(sessionsTillNow: Future[Seq[WithId[Session]]]): Future[List[FeedbackReportHeader]] = {
    val currentDate = new Date()
    val reportHeader = sessionsTillNow map { allUserSessionsTillNow =>
      allUserSessionsTillNow map { session =>
        val active = currentDate.getTime < session.entity.expirationDate.getTime
        generateSessionReportHeader(session, active)
      }
    }
    reportHeader.map { feedbackReportHeaderList =>
      feedbackReportHeaderList.toList
    }

  }

  private def generateSessionReportHeader(session: WithId[Session], active: Boolean): FeedbackReportHeader = {
    val currentDate = new Date()
    FeedbackReportHeader(
      session.id,
      session.entity.topic,
      session.entity.email,
      active = active,
      session.entity.session,
      session.entity.meetup,
      new Date(session.entity.date.getTime).toString,
      session.entity.rating,
      new Date(session.entity.expirationDate.getTime).before(currentDate)
    )
  }

  def fetchUserResponsesBySessionId(
    sessionId: String,
    userId: String
  ): Future[Either[FeedbackFormReportServiceError, FeedbackReport]] = {
    val responses = feedbackFormsResponseDao.listAll(StoredSessionId(sessionId))
    userDao.get(userId).flatMap {
      case Some(userInformation) =>
        renderFetchedResponses(responses, sessionId, userInformation.entity.superUser).map { feedbackReportResult =>
          feedbackReportResult.asRight
        }
      case None => Future.successful(FeedbackFormReportServiceError.UserNotFound.asLeft)
    }
  }

  def fetchAllResponsesBySessionId(
    sessionId: String,
    userId: String
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[FeedbackFormReportServiceError, FeedbackReport]] =
    if (authorityUser.keycloakRole == Admin) {
      val responses = feedbackFormsResponseDao.listAll(StoredSessionId(sessionId))
      userDao.get(userId).flatMap { user =>
        val reportResult = renderFetchedResponses(responses, sessionId, user.get.entity.superUser)
        reportResult.map { feedbackReport =>
          feedbackReport.asRight
        }
      }
    } else
      Future.successful(FeedbackFormReportServiceError.FeedbackAccessDeniedError.asLeft)

  private def renderFetchedResponses(
    responses: Future[Seq[WithId[UserFeedbackResponse]]],
    id: String,
    isSuperUser: Boolean
  ): Future[FeedbackReport] = {
    val currentDate = new Date()
    sessionDao
      .get(id)
      .flatMap(_.fold {
        logger.error(s" No session found by $id")
        Future.successful(FeedbackReport(None, Nil))
      } { sessionInfo =>
        val header = FeedbackReportHeader(
          sessionInfo.id,
          sessionInfo.entity.topic,
          sessionInfo.entity.email,
          active = false,
          sessionInfo.entity.session,
          sessionInfo.entity.meetup,
          new Date(sessionInfo.entity.date.getTime).toString,
          sessionInfo.entity.rating,
          new Date(sessionInfo.entity.expirationDate.getTime).before(currentDate)
        )
        responses.map {
          sessionResponses =>
            if (sessionResponses.nonEmpty) {
              val questionAndResponses = sessionResponses.map(feedbackResponse =>
                if (isSuperUser)
                  UserFeedbackReport(
                    feedbackResponse.entity.email,
                    feedbackResponse.entity.coreMember,
                    feedbackResponse.entity.feedbackResponse
                  )
                else
                  UserFeedbackReport(
                    " ",
                    feedbackResponse.entity.coreMember,
                    feedbackResponse.entity.feedbackResponse
                  )
              )
              FeedbackReport(Some(header), questionAndResponses.toList)
            } else
              FeedbackReport(Some(header), Nil)
        }
      })
  }

}

object FeedbackFormReportService {

  sealed trait FeedbackFormReportServiceError

  object FeedbackFormReportServiceError {

    case object FeedbackError extends FeedbackFormReportServiceError
    case object FeedbackAccessDeniedError extends FeedbackFormReportServiceError
    case object FeedbackNotFoundError extends FeedbackFormReportServiceError
    case object UserNotFound extends FeedbackFormReportServiceError
  }

}
