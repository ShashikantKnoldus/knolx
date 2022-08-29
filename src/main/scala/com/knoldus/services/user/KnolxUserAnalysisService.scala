package com.knoldus.services.user

import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao.{
  CheckCoreMember,
  CheckFeedbackMail,
  CheckFeedbackScore,
  StoredSessionId
}
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.{ Active, Email }
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.EmailCheck
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.routes.contract.user.SessionsInformation
import com.knoldus.services.user.KnolxUserAnalysisService.KnolxUserAnalysisServiceError

import scala.concurrent.{ ExecutionContext, Future }

class KnolxUserAnalysisService(
  feedbackFormsResponseDao: FeedbackFormsResponseDao,
  sessionDao: SessionDao,
  userDao: UserDao
)(
  implicit val ec: ExecutionContext,
  implicit val logger: LoggingAdapter
) {

  def userSessionsResponseComparison(
    email: String
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[KnolxUserAnalysisServiceError, List[SessionsInformation]]] =
    if (authorityUser.keycloakRole == Admin) {
      val sessionInfo = sessionDao.listAll(And(Email(email), Active(true)))
      sessionInfo.flatMap { sessions: Seq[WithId[Session]] =>
        val sessionsInformation = sessions.map { session =>
          getScoreOfSession(session).map { scores =>
            val scoresWithoutZero = scores.filterNot(_ == 0)
            val sessionScore =
              if (scoresWithoutZero.nonEmpty)
                scoresWithoutZero.sum / scoresWithoutZero.length
              else 0.00
            SessionsInformation(
              session.entity.topic,
              session.entity.score,
              sessionScore
            )
          }
        }.toList

        val sessionsInformationList = Future.sequence(sessionsInformation)
        sessionsInformationList.map { _: List[SessionsInformation] => }
        sessionsInformationList.map(sessionsInformation => sessionsInformation.asRight)

      }
    } else
      Future.successful(KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError.asLeft)

  def getScoreOfSession(sessionInfo: WithId[Session]): Future[List[Double]] = {
    val sessionResponses =
      feedbackFormsResponseDao.listAll(And(StoredSessionId(sessionInfo.id), CheckCoreMember(false)))
    val scores = sessionResponses.map { userFeedbackResponse =>
      userFeedbackResponse.map { userResponse =>
        userResponse.entity.score
      }
    }
    scores.map { scoresList =>
      scoresList.toList
    }
  }

  def getBanCount(email: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[KnolxUserAnalysisServiceError, WithId[UserInformation]]] =
    if (authorityUser.keycloakRole == Admin) {
      val userInfo = userDao.get(EmailCheck(email))
      userInfo map {
        case Some(user) => user.asRight
        case _ => KnolxUserAnalysisServiceError.UserAnalysisNotFoundError.asLeft
      }
    } else
      Future.successful(KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError.asLeft)

  def getUserTotalKnolx(email: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[KnolxUserAnalysisServiceError, Map[String, Int]]] =
    if (authorityUser.keycloakRole == Admin) {
      val sessionInfo: Future[Seq[WithId[Session]]] = sessionDao.listAll(Email(email))
      val knolxCount = sessionInfo.map { sessions =>
        val sessionsList = sessions.map { session =>
          session.entity
        }
        if (sessionsList.nonEmpty)
          Map("totalKnolx" -> sessionsList.count(!_.meetup))
        else
          Map("totalKnolx" -> 0)
      }
      knolxCount.map { count =>
        count.asRight
      }
    } else
      Future.successful(KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError.asLeft)

  def getUserTotalMeetups(email: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError.type, Map[String, Int]]] =
    if (authorityUser.keycloakRole == Admin) {
      val sessionInfo: Future[Seq[WithId[Session]]] = sessionDao.listAll(Email(email))
      val meetupCounts = sessionInfo.map { sessions =>
        val sessionsList = sessions.map { session =>
          session.entity
        }
        if (sessionsList.nonEmpty)
          Map("totalMeetUps" -> sessionsList.count(_.meetup))
        else
          Map("totalMeetUps" -> 0)
      }
      meetupCounts.map { count =>
        count.asRight
      }
    } else
      Future.successful(KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError.asLeft)

  def getUserDidNotAttendSessionCount(
    email: String
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[KnolxUserAnalysisServiceError, Map[String, Int]]] =
    if (authorityUser.keycloakRole == Admin) {
      val didNotAttendCount = feedbackFormsResponseDao.count(And(CheckFeedbackMail(email), CheckFeedbackScore(0d)))
      didNotAttendCount.map { count =>
        Map("didNotAttendCount" -> count).asRight
      }
    } else
      Future.successful(KnolxUserAnalysisServiceError.UserAnalysisAccessDeniedError.asLeft)
}

object KnolxUserAnalysisService {

  sealed trait KnolxUserAnalysisServiceError

  object KnolxUserAnalysisServiceError {

    case object UserAnalysisError extends KnolxUserAnalysisServiceError

    case object UserAnalysisAccessDeniedError extends KnolxUserAnalysisServiceError

    case object UserAnalysisNotFoundError extends KnolxUserAnalysisServiceError

  }

}
