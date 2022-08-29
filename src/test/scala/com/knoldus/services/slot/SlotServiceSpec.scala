package com.knoldus.services.slot

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.holiday.HolidayDao
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.dao.slot.SlotDao
import com.knoldus.domain.session.{GetSlotsResponse, NewSession, SessionDetails}
import com.knoldus.domain.slot.Slot
import com.knoldus.domain.user.KeycloakRole.{Admin, Employee}
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.session.PresenterDetails
import com.knoldus.{BaseSpec, RandomGenerators}
import com.typesafe.config.Config
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

class SlotServiceSpec extends BaseSpec {

  trait Setup {
    val slotDao: SlotDao = mock[SlotDao]
    val newSessionDao: NewSessionDao = mock[NewSessionDao]
    val holidayDao: HolidayDao = mock[HolidayDao]
    val config: Config = mock[Config]
    val randomString: String = RandomGenerators.randomString()
    val randomId: String = randomString
    val testEmail = "test"
    val testName = "testing"
    val dateTest: Date = new Date(1575927000000L)
    val service = new SlotService(config, slotDao, newSessionDao, holidayDao)
    val slotDuration = 45

    val dateTime = 5656649365200L

    implicit val newAdminUserInformation: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Admin
      )

    implicit val newEmployeeUserInformation: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Employee
      )

    implicit val userInformationTest: NewUserInformation =
      NewUserInformation(
        testEmail,
        "name",
        Admin
      )

    val bookableSlotInfo: Slot =
      Slot(
        "Knlox",
        1L,
        bookable = true,
        createdBy = randomString,
        1L,
        sessionId = None,
        45
      )

    val bookedSlotInfo: Slot =
      Slot(
        "Knlox",
        1L,
        bookable = false,
        createdBy = randomString,
        1L,
        sessionId = Some(randomString),
        1
      )

    val newSession: NewSession = NewSession(
      PresenterDetails(
        "Testadmin",
        "testadmin@knoldus.com"
      ),
      Some(
        PresenterDetails(
          "Someone",
          "Something"
        )
      ),
      1655103142,
      45,
      "",
      "",
      "",
      null,
      1655317799,
      "Knolx",
      "PendingForAdmin",
      "Description",
      null,
      null,
      null,
      null,
      null,
      null,
      null
    )
    val startDate = 12135555L
    val id = "1"
    val fakeName = "Test"
    val fakeEmail = "Testing"

    val presenterDetails: PresenterDetails =
      PresenterDetails(
        fullName = fakeName,
        email = fakeEmail
      )

    val sessionDeatails: SessionDetails = SessionDetails(
      id,
      "Scala",
      presenterDetails,
      Some(presenterDetails)
    )

    val getSlotsResponse: GetSlotsResponse = GetSlotsResponse(
      id,
      startDate,
      bookable = true,
      "Admin",
      Some(sessionDeatails),
      45,
      "KNOLX"
    )
    val newSessionWithId: WithId[NewSession] = WithId(newSession, "1")
    val sessionWithId: WithId[SessionDetails] = WithId(sessionDeatails, "1")
    val bookableSlotWithId: WithId[Slot] = WithId(entity = bookableSlotInfo, randomId)
    val bookedSlotWithId: WithId[Slot] = WithId(entity = bookedSlotInfo, randomId)
    val updatedSlot: Slot = bookableSlotWithId.entity.copy(slotType = "Webinar", dateTime = dateTime, slotDuration = 30)
    val updatedSlotWithId: WithId[Slot] = WithId(updatedSlot, bookableSlotWithId.id)
  }

  "SlotService#CreateSlot" should {
    "create new slot" in new Setup {
      when(holidayDao.get(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      when(slotDao.create(any[Slot])(any[ExecutionContext])).thenReturn(Future("1234"))
      whenReady(service.createSlot("Knolx", dateTime)(newAdminUserInformation)) { result =>
        assert(result.isRight)
      }
    }

    "return an error if user is unauthorized" in new Setup {
      whenReady(service.createSlot("Knolx", dateTime)(newEmployeeUserInformation)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "SlotService#UpdateSlot" should {
    "update the slot successfully" in new Setup {
      when(holidayDao.get(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      when(slotDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(bookableSlotWithId)))
      when(slotDao.update(any[String], any[Slot => Slot].apply)(any[concurrent.ExecutionContext])).thenReturn(Future(Some(updatedSlotWithId)))
      whenReady(service.updateSlot(bookableSlotWithId.id, "Webinar", dateTime)(newAdminUserInformation)) { result =>
        assert(result.isRight)
      }
    }

    "return an error if slot does not exist" in new Setup {
      when(holidayDao.get(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      when(slotDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.updateSlot(bookableSlotWithId.id, "Webinar", dateTime)(newAdminUserInformation)) { result =>
        assert(result.isLeft)
      }
    }

    "return an error if user is unauthorized" in new Setup {
      whenReady(service.updateSlot(bookableSlotWithId.id, "Webinar", dateTime)(newEmployeeUserInformation)) { result =>
        assert(result.isLeft)
      }
    }
}

  "SlotService#DeleteSlot" should {
    "delete the slot if that slot is not booked" in new Setup {
      when(slotDao.get(any[String])(any[concurrent.ExecutionContext])).thenReturn(Future.successful(Some(bookableSlotWithId)))
      when(slotDao.delete(any[String])(any[concurrent.ExecutionContext])).thenReturn(future(true))
      whenReady(service.deleteSlot(randomId)(newAdminUserInformation)) { result =>
        assert(result.isRight)
      }
    }

    "return an error if the slot is booked" in new Setup {
      when(slotDao.get(any[String])(any[concurrent.ExecutionContext])).thenReturn(Future.successful(Some(bookedSlotWithId)))
      whenReady(service.deleteSlot(randomId)(newAdminUserInformation)) { result =>
        assert(result.isLeft)
      }
    }

    "return an error if the slot does not exist" in new Setup {
      when(slotDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.deleteSlot(randomId)(newAdminUserInformation)) { result =>
        assert(result.isLeft)
      }
    }

    "return an error if the user is unauthorized" in new Setup {
      whenReady(service.deleteSlot(randomId)(newEmployeeUserInformation)) { result =>
        assert(result.isLeft)
      }
    }
  }
}
