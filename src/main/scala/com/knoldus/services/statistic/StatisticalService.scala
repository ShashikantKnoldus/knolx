package com.knoldus.services.statistic

import cats.data.EitherT
import java.util.Date
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.{ Active, Completed, Email, EndDate, StartDate }
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.CheckActiveUsers
import com.knoldus.domain.session.Session
import scala.concurrent.{ ExecutionContext, Future }
import cats.implicits._
import com.knoldus.domain.statistic.{ KnolxDetails, Statistic }
import com.knoldus.domain.user.UserInformation
import com.knoldus.services.statistic.StatisticalService.StatisticalServiceError

class StatisticalService(sessionDao: SessionDao, userDao: UserDao)(implicit
  val ec: ExecutionContext
) {

  def getKnolxDetails(
    startDate: Option[Long] = None,
    endDate: Option[Long] = None
  ): Future[Either[StatisticalServiceError, Seq[Statistic]]] = {

    def getSessionsForGivenUserWithCount(userEmail: String): Future[(Seq[WithId[Session]], Int)] =
      (startDate, endDate) match {
        case (None, None) =>
          sessionDao.listAll(And(Active(true), And(Email(userEmail), Completed(new Date())))).flatMap { sessions =>
            sessionDao.count(And(Active(true), And(Email(userEmail), Completed(new Date())))).map { count =>
              (sessions, count)
            }
          }
        case (Some(startDate), Some(endDate)) =>
          sessionDao
            .listAll(
              And(
                Active(true),
                And(
                  Email(userEmail),
                  And(Completed(new Date()), And(StartDate(new Date(startDate)), EndDate(new Date(endDate))))
                )
              )
            )
            .flatMap { sessions =>
              sessionDao
                .count(
                  And(
                    Active(true),
                    And(
                      Email(userEmail),
                      And(Completed(new Date()), And(StartDate(new Date(startDate)), EndDate(new Date(endDate))))
                    )
                  )
                )
                .map { count =>
                  (sessions, count)
                }
            }
        case _ => throw new RuntimeException("Either start date or end date is not provided")
      }

    def validateDates: Either[StatisticalServiceError, Unit] =
      if (startDate.isDefined && endDate.isDefined && new Date(startDate.get).after(new Date(endDate.get)))
        StatisticalServiceError.InvalidDates.asLeft
      else
        ().asRight

    def getStatistics: Future[Seq[Statistic]] =
      userDao.listAll(CheckActiveUsers(true)).flatMap { usersWithId =>
        Future.sequence(usersWithId.map {
          case WithId((UserInformation(userEmail, _, _, _, _, _, _, _, _, _)), userId) =>
            getSessionsForGivenUserWithCount(userEmail).map {
              case (sessions, sessionCount) =>
                val knolxDetails = sessions.map { session =>
                  KnolxDetails(session.id, session.entity.topic, session.entity.date.toString)
                }
                Statistic(userId, userEmail, sessionCount, knolxDetails)
            }
        })
      }

    val result: EitherT[Future, StatisticalServiceError, Seq[Statistic]] = for {
      _ <- EitherT.fromEither[Future](validateDates)
      statistics <- EitherT.right[StatisticalServiceError](getStatistics)
    } yield statistics
    result.value
  }
}

object StatisticalService {

  sealed trait StatisticalServiceError

  object StatisticalServiceError {

    case object InvalidDates extends StatisticalServiceError

  }

}
