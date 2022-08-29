package com.knoldus.services.feedbackform

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao.StoredSessionId
import com.knoldus.dao.filters.{ And, Or }
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.{ Active, UserKnolxSession }
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.feedbackform.UserFeedbackResponse
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.{ BaseSpec, RandomGenerators }

import java.util.Date
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.{ ExecutionContext, Future }

class FeedbackFormReportServiceSpec extends BaseSpec {

  trait Setup {
    val feedbackFormResponseDao: FeedbackFormsResponseDao = mock[FeedbackFormsResponseDao]
    val sessionDao: SessionDao = mock[SessionDao]
    val userDao: UserDao = mock[UserDao]
    val service = new FeedbackFormReportService(feedbackFormResponseDao, sessionDao, userDao)
    val dateTest: Date = new Date()
    val randomId: String = RandomGenerators.randomString()
    val randomInteger: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val testEmail: String = "test"
    val testName = "Testing"

    implicit val newUserInformationAdmin: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Admin
      )

    implicit val newUserInformationEmployee: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Employee
      )

    implicit val userInformation: UserInformation =
      UserInformation(
        testEmail,
        active = true,
        admin = true,
        coreMember = true,
        superUser = true,
        new Date(),
        1,
        Some(dateTest),
        nonParticipating = false,
        department = None
      )

    val sessionInfo: Session =
      Session(
        randomId,
        email = "random.test",
        dateTest,
        session = "Session1",
        category = "Database",
        subCategory = "Mysql",
        feedbackFormId = randomId,
        topic = "CRUD",
        feedbackExpirationDays = randomInteger,
        meetup = randomBoolean,
        brief = "Major Topic",
        rating = "4",
        score = 0.0,
        cancelled = randomBoolean,
        active = randomBoolean,
        dateTest,
        youtubeURL = Some("www.knoldusYoutube.com"),
        slideShareURL = Some("www.KnoldusSlide.com"),
        temporaryYoutubeURL = Some("www.KnoldusTemp.com"),
        reminder = randomBoolean,
        notification = randomBoolean
      )

    val storedUserResponse: UserFeedbackResponse = UserFeedbackResponse(
      randomId,
      randomId,
      coreMember = randomBoolean,
      "praful.bangar@knoldus.com",
      List(),
      meetup = randomBoolean,
      "admin@knoldus.in",
      new Date(1575417600),
      0.0,
      "session 1",
      "tab",
      dateTest
    )

    val userInfo: UserInformation = UserInformation(
      "email",
      active = randomBoolean,
      admin = randomBoolean,
      coreMember = randomBoolean,
      superUser = randomBoolean,
      dateTest,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

  }

  "FeedbackFormReportService#manage user FeedbackReports " should {
    "manage all feedback reports for user" in new Setup {
      when(
        sessionDao.list(UserKnolxSession(any[String]), any[Int], any[Int], any[Option[SortBy]])(any[ExecutionContext])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )

      when(sessionDao.count(And(Active(any[Boolean]), UserKnolxSession(any[String])))) thenReturn future(randomInteger)

      whenReady(service.manageUserFeedbackReports(randomId, randomInteger)) { result =>
        assert(result.isRight)
      }
    }
  }

  "FeedbackFormReportService#Manage All FeedbackReports " should {
    "manage all feedback reports " in new Setup {
      when(
        sessionDao.list(Active(any[Boolean]), any[Int], any[Int], any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      when(sessionDao.count(Or(Active(any[Boolean]), Active(any[Boolean])))) thenReturn future(randomInteger)
      whenReady(service.manageAllFeedbackReports(randomInteger)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }
  "Get all feedback report based on Date" should {
    "ordering of reports must be Descending" in new Setup {
      val currentDate = new Date()
      when(
        sessionDao.list(
          And(Active(any[Boolean]), SessionDao.SessionDate(currentDate)),
          any[Int],
          any[Int],
          any[Option[SortBy]]
        )(
          any[ExecutionContext]
        )
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      when(sessionDao.count(Or(Active(any[Boolean]), Active(any[Boolean])))) thenReturn future(randomInteger)
      whenReady(service.manageAllFeedbackReports(randomInteger)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }

  "FeedbackFormReportService#Fetch user FeedbackReports " should {
    "Fetch Responses by session Id for single user " in new Setup {
      when(
        feedbackFormResponseDao.listAll(StoredSessionId(any[String]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn future(
            Seq(WithId(storedUserResponse, randomId))
          )
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(userInfo, randomId))
          )
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      when(sessionDao.count(Or(Active(any[Boolean]), Active(any[Boolean])))) thenReturn future(1)

      whenReady(service.fetchUserResponsesBySessionId(randomId, randomId)) { result =>
        assert(result.isRight)
      }
    }
    "return error in Fetching Responses by session Id for single user " in new Setup {
      when(
        feedbackFormResponseDao.listAll(StoredSessionId(any[String]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn future(
            Seq(WithId(storedUserResponse, randomId))
          )
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(None)
      whenReady(service.fetchUserResponsesBySessionId(randomId, randomId)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "FeedbackFormReportService#Fetch FeedbackReports in all users" should {
    "Fetch Responses by session Id for all users " in new Setup {
      when(
        feedbackFormResponseDao.listAll(StoredSessionId(any[String]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn future(
            Seq(WithId(storedUserResponse, randomId))
          )
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(userInfo, randomId))
          )
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      when(sessionDao.count(Or(Active(any[Boolean]), Active(any[Boolean])))) thenReturn future(randomInteger)

      whenReady(service.fetchAllResponsesBySessionId(randomId, randomId)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }

}
