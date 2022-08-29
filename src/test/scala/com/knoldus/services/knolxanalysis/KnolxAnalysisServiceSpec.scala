package com.knoldus.services.knolxanalysis

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.session.{ CategoryDao, SessionDao }
import com.knoldus.dao.sorting.SortBy
import com.knoldus.domain.session.{ Category, Session }
import com.knoldus.domain.user.UserInformation
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class KnolxAnalysisServiceSpec extends BaseSpec {

  trait Setup {

    val sessionDao: SessionDao = mock[SessionDao]
    val categoryDao: CategoryDao = mock[CategoryDao]
    val service = new KnolxAnalysisService(sessionDao, categoryDao)
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val randomId: String = randomString
    val dateTest: Date = new Date(1575927000000L)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")
    val startDate: String = dateFormat.format(new Date())
    val endDate: String = dateFormat.format(new Date())
    val category: Category = Category(randomString, Seq(randomString))
    val withIdCategory: WithId[Category] = WithId(category, randomString)
    val testEmail = "test"
    val emailTest: Option[String] = Some("")
    val sessionStartDate: Date = new Date()
    val sessionEndDate: Date = new Date()

    implicit val userInformation: UserInformation =
      UserInformation(
        testEmail,
        active = true,
        admin = true,
        coreMember = true,
        superUser = true,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )

    val sessionInfo: Session =
      Session(
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
        dateTest,
        youtubeURL = Some(randomString),
        slideShareURL = Some(randomString),
        temporaryYoutubeURL = Some(randomString),
        reminder = randomBoolean,
        notification = randomBoolean
      )
    val withId: WithId[Session] = WithId(entity = sessionInfo, randomId)
  }

  "KnolxAnalysisService#GetSessioninRange" should {

    "return sessions in given range" in new Setup {

      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(service.getSessionInRange(startDate, endDate)) { result =>
        assert(result.isRight)
      }
    }
  }

  "KnolxAnalysisService#DoCategoryAnalysis" should {

    "return category information" in new Setup {
      when(categoryDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdCategory)))
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(service.doCategoryAnalysis(startDate, endDate)) { result =>
        assert(result.isRight)
      }
    }
  }

  "KnolxAnalysisService#DoMonthlySessionAnalysis" should {

    "return monthly sessions" in new Setup {

      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(service.doKnolxMonthlyAnalysis(startDate, endDate)) { result =>
        assert(result.isRight)
      }
    }
  }

  "KnolxAnalysisService#SessionsInTimeRange" should {

    "return sessions in a given TimeRange" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(service.sessionsInTimeRange(emailTest, sessionStartDate, sessionEndDate)) { result =>
        result.isRight
      }
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(service.sessionsInTimeRange(None, sessionStartDate, sessionEndDate)) { result =>
        result.isRight
      }
    }
  }

  "KnolxAnalysisService#SessionsInTimeRangeForNoneCase" should {

    "return error in geeting sessions for a TimeRange" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq()))
      whenReady(service.sessionsInTimeRange(emailTest, sessionStartDate, sessionEndDate)) { result =>
        result.isLeft
      }
    }
  }

}
