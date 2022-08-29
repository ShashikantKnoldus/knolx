package com.knoldus.services.session

import akka.actor.{ActorRef, Props}
import com.google.api.services.directory.Directory
import com.knoldus.{BaseSpec, RandomGenerators}
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.dao.slot.SlotDao
import com.knoldus.dao.sorting.SortBy
import com.knoldus.domain.session.NewSession
import com.knoldus.domain.session.sessionState.PENDINGFORADMIN
import com.knoldus.domain.slot.Slot
import com.knoldus.domain.user.KeycloakRole.{Admin, Employee}
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.session.{BookSessionUserRequest, GetSessionByIdResponse, PresenterDetails}
import com.knoldus.services.email.MailerService
import com.knoldus.services.feedbackform.FeedbackFormResponseService
import com.knoldus.services.scheduler.{SchedulerService, SessionScheduler}
import com.knoldus.services.scheduler.SessionState.SessionState
import com.knoldus.services.usermanagement.UserManagementService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}


class NewSessionServiceSpec extends BaseSpec {

  trait Setup {
    val slotDao: SlotDao = mock[SlotDao]
    val newSessionDao: NewSessionDao = mock[NewSessionDao]
    val randomString: String = RandomGenerators.randomString()
    val slideURL: String = "www.example.com"
    val randomInt: Int = RandomGenerators.randomInt(10)
    val mockService = mock[Directory]
    val service = new NewSessionService(slotDao, newSessionDao, mockService)
    val schedulerService: SchedulerService = mock[SchedulerService]
    val userManagementService: UserManagementService = mock[UserManagementService]
    val feedbackFormResponseService: FeedbackFormResponseService = mock[FeedbackFormResponseService]
    val mailerService: MailerService = mock[MailerService]

    val bookSessionUserRequest: BookSessionUserRequest =
      BookSessionUserRequest("slotId", "topic", "category", "subCategory", "sessionType", "sessionStatus", None, None)
    val invalidBookSessionUserRequest: BookSessionUserRequest =
      BookSessionUserRequest("slotId", "", "category", "subCategory", "sessionType", "sessionStatus", None, None)

    val dateTest: Date = new Date

    val slot: Slot = Slot("Knolx", 1655103142, bookable = true, "Admin", 121212212, Some("sessionId"), 45)
    val slotWithId: WithId[Slot] = WithId(slot, "1")

    val getSessionByIdResponse: GetSessionByIdResponse = GetSessionByIdResponse(
      randomString,
      randomString,
      randomString,
      new Date(),
      randomString,
      randomString,
      randomString,
      randomString,
      randomString,
      randomString,
      1,
      meetup = true,
      randomString,
      1.0,
      cancelled = false,
      active = true,
      dateTest,
      randomString,
      randomString,
      Some(randomString),
      reminder = true,
      notification = true,
      randomString
    )
    when(schedulerService.sessionsForToday(any[SessionState])).thenReturn(Future(Right(List(getSessionByIdResponse))))

    lazy val sessionSchedulerActor: ActorRef = system.actorOf(
      Props(
        new SessionScheduler(schedulerService, userManagementService, feedbackFormResponseService, conf, mailerService)
      )
    )
    when(schedulerService.updateMailStatus(any[String], any[String])).thenReturn(Future(true))
    when(userManagementService.getActiveAndUnBannedEmails).thenReturn(Future(Right(List("random.knoldus.com"))))
    val dateLong: Long = 123456L
    val testEmail = "test"
    val testName = "Testing"

    implicit val userInformation: NewUserInformation =
      NewUserInformation("test",
        "test",
        Admin
      )
    implicit val userInformationTest: NewUserInformation =
      NewUserInformation(randomString,
        randomString,
        Admin
      )
    implicit val testUserInformation: NewUserInformation =  userInformationTest.copy(email = "testadmin@knoldus.com")
    val presenterDetails : PresenterDetails =
      PresenterDetails(
        fullName = randomString,
        email = randomString
      )

    val sessionInfo: NewSession = NewSession(
      presenterDetails = presenterDetails,
      coPresenterDetails = Some(presenterDetails),
      dateLong,
      45,
      topic = randomString,
      category = randomString,
      subCategory = randomString,
      feedbackFormId = Some(randomString),
      dateLong,
      sessionType = randomString,
      sessionState = "PendingForAdmin",
      sessionDescription = randomString,
      youtubeURL = Some(randomString),
      slideShareURL = Some(randomString),
      slideURL = Some(randomString),
      slidesApprovedBy = Some(randomString),
      sessionApprovedBy = Some(randomString),
      sessionTag = List(randomString),
      remarks = None
    )
    val withIdSession: WithId[NewSession] = WithId(entity = sessionInfo, randomString)
    val withIdSeq: Seq[WithId[NewSession]] = Seq(withIdSession)

    implicit val newUserInformationAdmin: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Admin
      )
    val tag: String = "tag"
    implicit val newUserInformationEmployee: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Employee
      )
  }
  "NewSessionService#getApprovedSessions" should {

    "return upcoming approved sessions" in new Setup {
      when(newSessionDao.list(any[Filter],any[Int],any[Int],any[Option[SortBy]])(any[ExecutionContext])) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getApprovedSessions(1,10)(userInformationTest)) { result =>
        assert(result.isRight)
      }
    }
    "return error" in new Setup {
      when(newSessionDao.list(any[Filter],any[Int],any[Int],any[Option[SortBy]])(any[ExecutionContext])) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getApprovedSessions(1,10)(newUserInformationEmployee)) { result =>
        assert(result.isLeft)
      }
    }
  }
  "NewSessionService#getRequestedSessions" should {

    "return sessions pending for approval" in new Setup {
      when(newSessionDao.list(any[Filter],any[Int],any[Int],any[Option[SortBy]])(any[ExecutionContext])) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getRequestedSessions(1,10)(userInformationTest)) { result =>
        assert(result.isRight)
      }
    }
    "return error" in new Setup {
      when(newSessionDao.list(any[Filter],any[Int],any[Int],any[Option[SortBy]])(any[ExecutionContext])) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getRequestedSessions(1,10)(newUserInformationEmployee)) { result =>
        assert(result.isLeft)
      }
    }
  }
  "NewSessionService#fetchSessionTags" should {

    "return matching tags" in new Setup {
      when(newSessionDao.fetchTags(any[String])) thenReturn future(Option(Seq(tag)))
      whenReady(service.fetchSessionTags(tag)) { result =>
        assert(result.isRight)
      }
    }

    "return error" in new Setup {
      when(newSessionDao.fetchTags(any[String])) thenReturn future(None)
      whenReady(service.fetchSessionTags(tag)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "NewSessionService#getSessions" should {

    "return upcoming sessions" in new Setup {
      when(
        newSessionDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[ExecutionContext])
      ) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getUpcomingSessions(randomInt, randomInt, Some(testEmail))) { result =>
        assert(result.isRight)
      }
    }

    "return past sessions" in new Setup {
      when(
        newSessionDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[ExecutionContext])
      ) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getPastSessions(randomInt, randomInt, Some(testEmail))) { result =>
        assert(result.isRight)
      }
    }
  }

  "NewSessionService#getUserSessions" should {

    "return user upcoming sessions" in new Setup {
      when(
        newSessionDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[ExecutionContext])
      ) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getUserSessions(randomInt, randomInt, "upcoming")(userInformation)) { result =>
        assert(result.isRight)
      }
    }

    "return user past sessions" in new Setup {
      when(
        newSessionDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[ExecutionContext])
      ) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getUserSessions(randomInt, randomInt, "past")(userInformation)) { result =>
        assert(result.isRight)
      }
    }

    "return error" in new Setup {
      when(
        newSessionDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[ExecutionContext])
      ) thenReturn future(withIdSeq)
      when(newSessionDao.count(any[Filter])(any[ExecutionContext])) thenReturn future(1)
      whenReady(service.getUserSessions(randomInt, randomInt, "completed")(userInformation)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "NewSessionService#bookSession" should {
    "return booked session" in new Setup {
      when(slotDao.get(any[String])(any[ExecutionContext])) thenReturn future(Option(slotWithId))
      when(slotDao.update(any[String], any[Slot => Slot].apply)(any[ExecutionContext])) thenReturn future(
            Option(slotWithId)
          )
      when(newSessionDao.create(any[NewSession])(any[ExecutionContext])) thenReturn future("id")
      whenReady(service.bookSession(bookSessionUserRequest)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "return mandatory field not found error" in new Setup {
      when(slotDao.get(any[String])(any[ExecutionContext])) thenReturn future(Option(slotWithId))
      when(slotDao.update(any[String], any[Slot => Slot].apply)(any[ExecutionContext])) thenReturn future(
        Option(slotWithId)
      )
      when(newSessionDao.create(any[NewSession])(any[ExecutionContext])) thenReturn future("id")
      whenReady(service.bookSession(invalidBookSessionUserRequest)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }

    "return error" in new Setup {
      when(slotDao.get(any[String])(any[ExecutionContext])) thenReturn future(None)
      whenReady(service.bookSession(bookSessionUserRequest)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }

    "NewSessionService#GetSessionbyId" should {

      "return session by Id" in new Setup {
        when(newSessionDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withIdSession)))
        whenReady(service.getNewSession("1")(newUserInformationAdmin)) { result =>
          assert(result.isRight)
        }
      }
      "return error department by Id" in new Setup {
        when(newSessionDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
        whenReady(service.getNewSession("1")(newUserInformationAdmin)) { result =>
          assert(result.isLeft)
        }
      }

    }
  }
  "NewSessionService#UpdateSession" should {

    "give updated session" in new Setup {
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])).thenReturn(Future(Option(withIdSession)))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withIdSession)))
      whenReady(
        service.updateNewSessions(
          randomString,
          Some(presenterDetails),
          Some(dateLong),
          Some(randomString),
          Some(randomString),
          Some(randomString),
          Some(randomString),
          Some(randomString),
          Some(randomString)
        )(userInformation)
      ) { result =>
        assert(result.isRight)
      }

    }
  }

  "NewSessionService#UpdateSession" should {

    "return session not found error" in new Setup {
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withIdSession)))
      whenReady(
        service.updateNewSessions(
          randomString,
          Some(presenterDetails),
          Some(dateLong),
          Some(randomString),
          Some(randomString),
          Some(randomString),
          Some(randomString),
          Some(randomString),
          Some(randomString)
        )(userInformation)
      ) { result =>
        assert(result.isLeft)
      }
    }
  }
  "Update users session" should {

    "give session with updated details" in new Setup {
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])).thenReturn(Future(Option(withIdSession)))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withIdSession)))
      whenReady(
        service.updateUserSession(
          randomString,
          Some(randomString),
          Some(randomString),
          Some(slideURL)
        )(userInformationTest)
      ) { result =>
        assert(result.isRight)
      }
    }
    "give session with single updated details" in new Setup {
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])).thenReturn(Future(Option(withIdSession)))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withIdSession)))
      whenReady(
        service.updateUserSession(
          randomString,
          Some(randomString),
          None,
          None)(userInformationTest)
      ) { result =>
        assert(result.isRight)
      }
    }
    "return session not found error" in new Setup {
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withIdSession)))
      whenReady(
        service.updateUserSession(
          randomString,
          Some(randomString),
          Some(randomString),
          Some(randomString)
        )(userInformation)
      ) { result =>
        assert(result.isLeft)
      }
    }
  }

}
