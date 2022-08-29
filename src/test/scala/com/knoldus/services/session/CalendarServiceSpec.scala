package com.knoldus.services.session

import akka.Done
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, Filter, Or }
import com.knoldus.dao.session.CalenderDao
import com.knoldus.dao.session.CalenderDao.{ Declined, FreeSlot, Notification }
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{ Admin, SuperUser }
import com.knoldus.domain.session.SessionRequest
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.routes.contract.session.{ AddSlotRequest, DeclineSessionRequest, UpdateApproveSessionInfo }
import com.knoldus.services.email.MailerService
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class CalendarServiceSpec extends BaseSpec {

  trait Setup {
    val calendarDao: CalenderDao = mock[CalenderDao]
    val userDao: UserDao = mock[UserDao]
    val mailerService: MailerService = mock[MailerService]
    val service = new CalendarService(calendarDao, userDao, mailerService)
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val dateTest = new Date
    val addSlotRequest: AddSlotRequest = AddSlotRequest(randomString, dateTest, isNotification = false)
    val addSlotRequestNotification: AddSlotRequest = AddSlotRequest(randomString, dateTest, isNotification = true)
    val declineSessionRequest: DeclineSessionRequest = DeclineSessionRequest(approved = false, declined = true)
    val testEmail = "test"
    val testName = "testing"
    import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }

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
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )

    val updateApproveSessionInfo: UpdateApproveSessionInfo =
      UpdateApproveSessionInfo(dateTest)

    val sessionRequest: SessionRequest = SessionRequest(
      approved = randomBoolean,
      category = randomString,
      date = dateTest,
      decline = randomBoolean,
      email = randomString,
      freeSlot = randomBoolean,
      meetup = randomBoolean,
      notification = randomBoolean,
      recommendationId = randomString,
      subCategory = randomString,
      topic = randomString,
      brief = randomString
    )

    val userInfo: UserInformation = UserInformation(
      randomString,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      randomBoolean,
      department = None
    )

    val userInform: UserInformation =
      UserInformation(
        testEmail,
        active = true,
        admin = false,
        coreMember = true,
        superUser = false,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )

    val userWithId: WithId[UserInformation] = WithId(userInfo, randomString)
    val withId: WithId[SessionRequest] = WithId(sessionRequest, randomString)
  }

  "CalendarService#InsertFreeSlot" should {

    "insert free slot" in new Setup {
      when(calendarDao.create(any[SessionRequest])(any[ExecutionContext])).thenReturn(Future(""))
      whenReady(service.insertSlot(addSlotRequest)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in insert free slot" in new Setup {
      when(calendarDao.create(any[SessionRequest])(any[ExecutionContext])).thenReturn(Future(""))
      whenReady(service.insertSlot(addSlotRequest)(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CalendarService#InsertSlot" should {

    "insert notification" in new Setup {
      when(calendarDao.create(any[SessionRequest])(any[ExecutionContext])).thenReturn(Future(""))
      whenReady(service.insertSlot(addSlotRequestNotification)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#UpdateSessionForApprove" should {

    "update session" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(
        calendarDao.update(any[String], any[SessionRequest => SessionRequest].apply)(any[concurrent.ExecutionContext])
      ).thenReturn(Future(Option(withId)))
      whenReady(service.updateSessionForApprove("1", updateApproveSessionInfo)) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#UpdateSessionForApprove" should {

    "return error" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(
        calendarDao.update(any[String], any[SessionRequest => SessionRequest].apply)(any[concurrent.ExecutionContext])
      ).thenReturn(Future(Option(withId)))
      whenReady(service.updateSessionForApprove("1", updateApproveSessionInfo)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CalendarService#GetAllFreeSlot" should {

    "return all free slot" in new Setup {
      when(calendarDao.listAll(FreeSlot(any[Boolean]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withId)))
      whenReady(service.getAllFreeSlots) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#UpadetDateForPendingSession" should {

    "update date for pending session" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(
        calendarDao.update(any[String], any[SessionRequest => SessionRequest].apply)(any[concurrent.ExecutionContext])
      ).thenReturn(Future(Option(withId)))
      whenReady(service.updateDateForPendingSession("1", dateTest)) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#UpadetDateForPendingSession" should {

    "return session not found error" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(
        calendarDao.update(any[String], any[SessionRequest => SessionRequest].apply)(any[concurrent.ExecutionContext])
      ).thenReturn(Future(Option(withId)))
      whenReady(service.updateDateForPendingSession("1", dateTest)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CalendarService#GetAllSessionsWithSearch" should {

    "return all sessions" in new Setup {
      when(
        calendarDao.list(
          And(Notification(any[Boolean]), FreeSlot(any[Boolean])),
          any[Int],
          any[Int],
          any[Option[SortBy]]
        )
      ).thenReturn(Future(Seq(withId)))
      when(calendarDao.count(And(Notification(any[Boolean]), FreeSlot(any[Boolean])))).thenReturn(Future(1))
      whenReady(service.getAllSessionsForAdmin(randomInt, randomInt, Some(""))(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in getting all sessions" in new Setup {
      when(
        calendarDao.list(
          And(Notification(any[Boolean]), FreeSlot(any[Boolean])),
          any[Int],
          any[Int],
          any[Option[SortBy]]
        )
      ).thenReturn(Future(Seq(withId)))
      when(calendarDao.count(And(Notification(any[Boolean]), FreeSlot(any[Boolean])))).thenReturn(Future(1))
      whenReady(service.getAllSessionsForAdmin(randomInt, randomInt, Some(""))(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CalendarService#GetAllSessionsWithoutSearch" should {

    "return all sessions" in new Setup {
      when(
        calendarDao.list(
          And(Notification(any[Boolean]), FreeSlot(any[Boolean])),
          any[Int],
          any[Int],
          any[Option[SortBy]]
        )
      ).thenReturn(Future(Seq(withId)))
      when(calendarDao.count(And(Notification(any[Boolean]), FreeSlot(any[Boolean])))).thenReturn(Future(1))
      whenReady(service.getAllSessionsForAdmin(randomInt, randomInt, None)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#GetPendingSessionCount" should {

    "return no of pending session" in new Setup {
      when(
        calendarDao.count(
          And(FreeSlot(any[Boolean]), Declined(any[Boolean]))
        )
      ).thenReturn(Future(1))
      whenReady(service.getPendingSessions) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#GetSessionById" should {

    "return session of given id" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      whenReady(service.getSessionById("1")) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#GetSessionById" should {

    "return session not found error" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.getSessionById("1")) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CalendarService#DeclineSession" should {

    "decline given session" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(calendarDao.create(any[SessionRequest])(any[ExecutionContext])).thenReturn(Future(""))

      when(
        calendarDao.update(any[String], any[SessionRequest => SessionRequest].apply)(any[concurrent.ExecutionContext])
      ).thenReturn(Future(Option(withId)))
      whenReady(service.declineSession("1", declineSessionRequest)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "error in declining a given session" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(calendarDao.create(any[SessionRequest])(any[ExecutionContext])).thenReturn(Future(""))

      when(
        calendarDao.update(any[String], any[SessionRequest => SessionRequest].apply)(any[concurrent.ExecutionContext])
      ).thenReturn(Future(Option(withId)))
      whenReady(service.declineSession("1", declineSessionRequest)(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "ReturnErrorCalendarService#DeleteSlot" should {

    "error in deleting a slot of a given session" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.deleteSlot("1")) { result =>
        assert(result.isLeft)
      }
    }
    "delete slot of a given session" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(calendarDao.delete(any[String])(any[ExecutionContext])).thenReturn(Future(true))
      whenReady(service.deleteSlot("1")) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#DeclineSession" should {

    "return session not found error" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(
        calendarDao.update(any[String], any[SessionRequest => SessionRequest].apply)(any[concurrent.ExecutionContext])
      ).thenReturn(Future(Option(withId)))
      whenReady(service.declineSession("1", declineSessionRequest)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CalendarService#DeleteSlot" should {

    "delete slot" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(calendarDao.create(any[SessionRequest])(any[ExecutionContext])).thenReturn(Future(""))
      when(calendarDao.delete(any[String])(any[ExecutionContext])).thenReturn(Future(true))
      whenReady(service.declineSession("1", declineSessionRequest)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#DeleteSlot" should {

    "return slot not found error" in new Setup {
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(calendarDao.delete(any[String])(any[ExecutionContext])).thenReturn(Future(true))
      whenReady(service.declineSession("1", declineSessionRequest)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "SessionService#SendEmail" should {

    "send email to admin/superUser" in new Setup {
      when(userDao.listAll(Or(Admin(any[Boolean]), SuperUser(any[Boolean])))(any[ExecutionContext]))
        .thenReturn(Future(Seq(userWithId)))
      when(calendarDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      when(mailerService.sendMessage(any[String], any[String], any[String])(any[ExecutionContext]))
        .thenReturn(future(Done))
      whenReady(service.sendEmail("1")) { result =>
        assert(result.isRight)
      }
    }
  }

  "CalendarService#GetSessionInMonth" should {

    "return all sessions in given month" in new Setup {
      when(
        calendarDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])
      ).thenReturn(Future(Seq(withId)))
      whenReady(service.getSessionsInMonth(1L, 1L)) { result =>
        assert(result.isRight)
      }
    }
  }
}
