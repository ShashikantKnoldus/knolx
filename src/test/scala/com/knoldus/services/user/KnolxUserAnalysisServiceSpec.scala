package com.knoldus.services.user

import java.util.Date
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao.{ CheckFeedbackMail, CheckFeedbackScore, StoredSessionId }
import com.knoldus.dao.feedbackform.{ FeedbackFormDao, FeedbackFormsResponseDao }
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.Active
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{ EmailCheck, EmailFilter }
import com.knoldus.domain.feedbackform.UserFeedbackResponse
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.ExecutionContext

class KnolxUserAnalysisServiceSpec extends BaseSpec {

  trait Setup {
    val feedbackFormDao: FeedbackFormDao = mock[FeedbackFormDao]
    val feedbackFormResponseDao: FeedbackFormsResponseDao = mock[FeedbackFormsResponseDao]
    val sessionDao: SessionDao = mock[SessionDao]
    val userDao: UserDao = mock[UserDao]
    val dateTest: Date = new Date()
    val randomId: String = RandomGenerators.randomString()
    val randomInteger: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val service = new KnolxUserAnalysisService(feedbackFormResponseDao, sessionDao, userDao)
    val testEmail = "test"
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
        true,
        true,
        true,
        true,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
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
        feedbackFormId = "1",
        topic = "CRUD",
        feedbackExpirationDays = randomInteger,
        meetup = true,
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
      8.0,
      "session 1",
      "tab",
      dateTest
    )

    val userEntity: UserInformation = UserInformation(
      "email",
      active = randomBoolean,
      admin = randomBoolean,
      coreMember = randomBoolean,
      superUser = randomBoolean,
      dateTest,
      lastBannedOn = Some(dateTest),
      nonParticipating = randomBoolean,
      department = None
    )

    val userInfo: UserInformation =
      UserInformation(
        testEmail,
        true,
        false,
        true,
        false,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )

  }

  "KnolxUserAnalysisService#Compare User Session Feedback Responses " should {
    "compare responses from core member and non core member" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      when(
        feedbackFormResponseDao.listAll(StoredSessionId(any[String]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn future(
            Seq(WithId(storedUserResponse, randomId))
          )

      whenReady(service.userSessionsResponseComparison("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "error in comparing a responses from core member and non core member" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      when(
        feedbackFormResponseDao.listAll(StoredSessionId(any[String]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn future(
            Seq(WithId(storedUserResponse, randomId))
          )

      whenReady(service.userSessionsResponseComparison("random@knoldus.com")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "KnolxUserAnalysisService#get Total Meetups Sessions User" should {
    "get total meetups sessions given by users" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      whenReady(service.getUserTotalMeetups("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }

  "KnolxUserAnalysisService#get Total Meetups Sessions User" should {
    "get total meetups sessions given by users for empty seq" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq()
          )
      whenReady(service.getUserTotalMeetups("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in getting total meetups sessions given by users for empty seq" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq()
          )
      whenReady(service.getUserTotalMeetups("random@knoldus.com")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "KnolxUserAnalysisService#get Total Knolx sessions" should {
    "get total knolx sessions given by users" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      whenReady(service.getUserTotalKnolx("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in total knolx sessions given by users" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      whenReady(service.getUserTotalKnolx("random@knoldus.com")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "KnolxUserAnalysisService#get Total Knolx sessions" should {
    "get total knolx sessions given by users for empty seq" in new Setup {
      when(
        sessionDao.listAll(And(EmailFilter(any[String]), Active(any[Boolean])), any[Option[SortBy]])
      ) thenReturn future(
            Seq()
          )
      whenReady(service.getUserTotalKnolx("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }

  "KnolxUserAnalysisService#get TotalBan Count of User" should {
    "get total ban Count of Users by MailId" in new Setup {
      when(userDao.get(EmailCheck(any[String]))(any[ExecutionContext])) thenReturn future(
            Some(WithId(userEntity, randomId))
          )
      whenReady(service.getBanCount("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "access denied in getting total ban Count of Users by MailId" in new Setup {
      when(userDao.get(EmailCheck(any[String]))(any[ExecutionContext])) thenReturn future(
            Some(WithId(userEntity, randomId))
          )
      whenReady(service.getBanCount("random@knoldus.com")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "KnolxUserAnalysisService#get TotalBan Count of User" should {
    "return error " in new Setup {
      when(userDao.get(EmailCheck(any[String]))(any[ExecutionContext])) thenReturn future(
            None
          )
      whenReady(service.getBanCount("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "KnolxUserAnalysisService#get Not attended sessions Count of User" should {
    "get total not attended sessions Count of Users by MailId" in new Setup {
      when(
        feedbackFormResponseDao.count(And(CheckFeedbackMail(any[String]), CheckFeedbackScore(any[Double])))
      ) thenReturn future(
            2
          )
      whenReady(service.getUserDidNotAttendSessionCount("random@knoldus.com")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in getting total not attended sessions Count of Users by MailId" in new Setup {
      when(
        feedbackFormResponseDao.count(And(CheckFeedbackMail(any[String]), CheckFeedbackScore(any[Double])))
      ) thenReturn future(
            2
          )
      whenReady(service.getUserDidNotAttendSessionCount("random@knoldus.com")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

}
