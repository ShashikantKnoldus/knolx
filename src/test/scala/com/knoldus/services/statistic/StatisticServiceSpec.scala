package com.knoldus.services.statistic

import java.util.Date

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.session.Session
import com.knoldus.domain.statistic.{ KnolxDetails, Statistic }
import com.knoldus.domain.user.UserInformation
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import scala.concurrent.Future

class StatisticServiceSpec extends BaseSpec {

  trait Setup {
    val sessionDao: SessionDao = mock[SessionDao]
    val userDao: UserDao = mock[UserDao]
    val service = new StatisticalService(sessionDao, userDao)
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val date: Date = mock[Date]
    val dateTest: Date = new Date(1593491700000L)

    val session: Session = Session(
      userId = randomString,
      email = randomString,
      dateTest,
      session = randomString,
      category = randomString,
      subCategory = randomString,
      feedbackFormId = randomString,
      topic = randomString,
      feedbackExpirationDays = randomInt,
      meetup = randomBoolean,
      brief = randomString,
      rating = randomString,
      score = 0.0,
      cancelled = randomBoolean,
      active = randomBoolean,
      date,
      youtubeURL = Some(randomString),
      slideShareURL = Some(randomString),
      temporaryYoutubeURL = Some(randomString),
      reminder = randomBoolean,
      notification = randomBoolean
    )

    val withIdSession: WithId[Session] = WithId(session, randomString)

    val userInfo: UserInformation = UserInformation(
      "test",
      active = true,
      admin = false,
      coreMember = false,
      superUser = false,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val withIdUser: WithId[UserInformation] = WithId(userInfo, randomString)
  }

  "StatisticalService#getKnolxDetails" should {

    "Give knolx details for all" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))

      when(sessionDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(2))

      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser)))

      whenReady(service.getKnolxDetails()) { result =>
        result shouldBe Right(
          List(Statistic(randomString, "test", 2, List(KnolxDetails(randomString, randomString, dateTest.toString))))
        )
      }
    }

    "Give knolx details for specific period of time" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))

      when(sessionDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(2))

      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser)))

      whenReady(service.getKnolxDetails(Some(1590969600000L), Some(1594512000000L))) { result =>
        result shouldBe Right(
          List(Statistic(randomString, "test", 2, Seq(KnolxDetails(randomString, randomString, dateTest.toString))))
        )
      }
    }
  }

}
