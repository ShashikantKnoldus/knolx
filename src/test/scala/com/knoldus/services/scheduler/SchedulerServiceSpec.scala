package com.knoldus.services.scheduler

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.feedbackform.UserFeedbackResponse
import com.knoldus.domain.session.{ Session, SessionUnattendedUserInfo }
import com.knoldus.domain.user.UserInformation
import com.knoldus.services.scheduler.SessionState.{
  ExpiringNext,
  ExpiringNextNotReminded,
  SchedulingNext,
  SchedulingNextUnNotified
}
import com.knoldus.{ BaseSpec, RandomGenerators }
import java.time.{ LocalDate, ZoneId }
import java.util.{ Date, UUID }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import scala.concurrent.{ ExecutionContext, Future }

class SchedulerServiceSpec extends BaseSpec {

  trait Setup {
    val sessionDao: SessionDao = mock[SessionDao]
    val feedbackFormsResponseDao: FeedbackFormsResponseDao = mock[FeedbackFormsResponseDao]
    val userDao: UserDao = mock[UserDao]

    val schedulerService: SchedulerService =
      new SchedulerService(sessionDao, feedbackFormsResponseDao, userDao)
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val dateTest: Date = new Date(1575927000000L)
    val randomId: String = UUID.randomUUID().toString
    val currentDate: LocalDate = LocalDate.now()
    val previousMonthDate: LocalDate = currentDate.minusMonths(1)
    val startDate = new Date(previousMonthDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toEpochSecond * 1000)
    val endDate = new Date(currentDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toEpochSecond * 1000)

    val sessionUnattendedUserInfo = List(
      SessionUnattendedUserInfo("topic1", List("abc@knoldus.com", "xyz@knoldus.com")),
      SessionUnattendedUserInfo("topic2", List("xyz@knoldus.com", "pqr@knoldus.com"))
    )

    val userInfo: UserInformation = UserInformation(
      "test",
      active = true,
      admin = false,
      coreMember = true,
      superUser = false,
      dateTest,
      randomInt,
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

    val userInfoWithId: WithId[UserInformation] = WithId(userInfo, randomString)

    val withId: WithId[Session] = WithId(entity = sessionInfo, randomString)

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
  }

  "SchedulerService#getSessionsInMonth" should {
    "return sessions of given month" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(schedulerService.getSessionsInMonth(startDate, endDate)) { result =>
        result === List(withId)
      }
    }
  }

  "SchedulerService#ExpiringNext sessionsForToday " should {
    " return today's ExpiringNext sessions" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(schedulerService.sessionsForToday(ExpiringNext)) { result =>
        assert(result.isRight)
      }
    }
  }

  "SchedulerService#ExpiringNextNotReminded sessionsForToday " should {
    " return today's ExpiringNextNotReminded sessions" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(schedulerService.sessionsForToday(ExpiringNextNotReminded)) { result =>
        assert(result.isRight)
      }
    }
  }

  "SchedulerService#SchedulingNext sessionsForToday " should {
    "return today's SchedulingNext sessions" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(schedulerService.sessionsForToday(SchedulingNext)) { result =>
        assert(result.isRight)
      }
    }
  }

  "SchedulerService#SchedulingNextUnNotified sessionsForToday " should {
    "return today's SchedulingNextUnNotified sessions" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))
      whenReady(schedulerService.sessionsForToday(SchedulingNextUnNotified)) { result =>
        assert(result.isRight)
      }
    }
  }

  "SchedulerService#update Mail status " should {
    "update notification" in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      when(sessionDao.update(any[String], any[Session => Session].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(schedulerService.updateMailStatus(randomString, "Notification")) { result =>
        assert(result)
      }
    }
  }

  "SchedulerService#update Mail status " should {
    "return false" in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      when(sessionDao.update(any[String], any[Session => Session].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(None))
      whenReady(schedulerService.updateMailStatus(randomString, "Notification")) { result =>
        result === false
      }
    }
  }

  "SchedulerService#update Mail status " should {
    "update reminder " in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      when(sessionDao.update(any[String], any[Session => Session].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(schedulerService.updateMailStatus(randomString, "Reminder")) { result =>
        assert(result)
      }
    }
  }

  "SchedulerService#getAllNotAttendingUsers " should {
    "get list of all emails of not attending users" in new Setup {
      when(
        feedbackFormsResponseDao.listAll(any[Filter], any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(Seq(WithId(storedUserResponse, randomId)))
      whenReady(schedulerService.getAllNotAttendingResponseEmailsPerSession(randomId)) { result =>
        assert(result.isRight)
      }
    }
  }

  "Return Error SchedulerService#getAllNotAttendingUsers " should {
    "return error in getting list of all emails of not attending users" in new Setup {
      when(
        feedbackFormsResponseDao.listAll(any[Filter], any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(Seq(WithId(storedUserResponse, randomId)))
      whenReady(schedulerService.getAllNotAttendingResponseEmailsPerSession(randomId)) { result =>
        result.isLeft
      }
    }
  }

  "SchedulerService# getSessionById" should {
    "return session by id " in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      whenReady(schedulerService.getSessionById(randomString)) { result =>
        assert(result.isRight)
      }
    }
  }

  "SchedulerService#getUsersDidNotAttendSession" should {
    "return list of user not attend session with topic" in new Setup {
      when(sessionDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withId)))

      when(
        feedbackFormsResponseDao.listAll(any[Filter], any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(Seq(WithId(storedUserResponse, randomId)))
      whenReady(schedulerService.getUsersNotAttendedLastMonthSession) { _ =>
        sessionUnattendedUserInfo
      }
    }
  }

  "SchedulerService# getSessionById" should {
    "return error " in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(schedulerService.getSessionById(randomString)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "SchedulerService# getAllBannedUsers" should {
    "return all banned users" in new Setup {
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(userInfoWithId)))
      whenReady(schedulerService.getAllBannedUsers) { result =>
        result.map { user =>
          user === Seq(userInfo)
        }
      }
    }
  }

  "SchedulerService#getAllCoreMembers" should {
    "return all core users" in new Setup {
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(userInfoWithId)))
      whenReady(schedulerService.getAllCoreMembers) { result =>
        result.map { user =>
          user === Seq(userInfo)
        }
      }
    }
  }

  "SchedulerService#getAllBannedUsersOfLastMonth" should {
    "return all banned users" in new Setup {
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(userInfoWithId)))
      whenReady(schedulerService.getAllBannedUsersOfLastMonth) { result =>
        result.map { user =>
          user === Seq()
        }
      }
    }
  }

}
