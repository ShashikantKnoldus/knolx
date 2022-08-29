package com.knoldus.routes.slot

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.dao.slot.SlotDao
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.session.{GetSlotsResponse, NewSession, SessionDetails}
import com.knoldus.domain.slot.Slot
import com.knoldus.domain.user.KeycloakRole.{Admin, Employee}
import com.knoldus.domain.user.{NewUserInformation, UserInformation, UserToken}
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.session._
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.scheduler.SchedulerService
import com.knoldus.services.scheduler.SessionScheduler.ScheduleSession
import com.knoldus.services.session.NewSessionService
import com.knoldus.services.slot.SlotService
import com.knoldus.services.slot.SlotService.SlotServiceError
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}

import java.util.Date
import scala.concurrent.Future

class SlotRouteSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: NewSessionService = mock[NewSessionService]
    val slotService: SlotService = mock[SlotService]
    val slotDao: SlotDao = mock[SlotDao]
    val newSessionDao: NewSessionDao = mock[NewSessionDao]
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val newSlotRoutes = new SlotRoutes(slotService, authorizationService)
    val routes: Route = new SlotRoutes(slotService, authorizationService).routes
    val id = "1"
    val sessionID = "12"
    val pageNumber = 1
    val pageSize = 10
    val filter = "filter"
    val category = ""
    val dateTest: Date = new Date
    val sampleDate: Date = new Date(1569492774160L)
    val startDate: Long = 1216534216
    val endDate: Long = 1216534215
    val longDate = 1234
    val dateTime = 5656649365200L
    val slotDuration = 45
    val testEmail = "test"
    val testName = "Testing"
    val dateTimeUtility = new DateTimeUtility
    val date = new Date(dateTimeUtility.toMillis(dateTimeUtility.localDateTimeIST.plusDays(1)))
    cache.remove("token")
    val validationDetails: NewUserInformation = NewUserInformation(testEmail, testName, Admin)
    val schedulerService: SchedulerService = mock[SchedulerService]
    val slotNotCreatedError: SlotServiceError = SlotServiceError.SlotNotCreatedError
    val slotNotUpdatedError: SlotServiceError = SlotServiceError.SlotNotUpdatedError
    val slotNotFoundError: SlotServiceError = SlotServiceError.SlotNotFoundError
    val slotBookedError: SlotServiceError = SlotServiceError.SlotBookedError
    val accessDeniedError: SlotServiceError = SlotServiceError.AccessDenied

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

    implicit val userInformationAdmin: NewUserInformation =
      NewUserInformation("test", "test", Admin)

    implicit val userInformationEmployee: NewUserInformation =
      NewUserInformation("test", "test", Employee)
    implicit val scheduleSession: ScheduleSession = ScheduleSession("sessionId")
    val result: Future[UserToken] = cache.getOrLoad("token", _ => Future(UserToken("token", date, userInformation)))

    val fakeEmail = "average.joe@knoldus.com"
    val fakeName = "Average Joe"

    val sessionStatusResponse: UpdateSuccessResponse = UpdateSuccessResponse(id)

    val presenterDetails: PresenterDetails =
      PresenterDetails(
        fullName = fakeName,
        email = fakeEmail
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

    val sessionDeatails: SessionDetails = SessionDetails(
      id,
      "Scala",
      presenterDetails,
      Some(presenterDetails)
    )

    val getSlotsResponse: GetSlotsResponse = GetSlotsResponse(
      id,
      startDate,
      true,
      "Admin",
      Some(sessionDeatails),
      45,
      "KNOLX"
    )
    val newSessionWithId: WithId[NewSession] = WithId(newSession, "1")
    val slot: Slot = Slot("Meetup", 1655103142, bookable = true, "Admin", 121212212, None, 45)
    val slotWithId: WithId[Slot] = WithId(slot, "1234")
    val sessionWithId: WithId[SessionDetails] = WithId(sessionDeatails, "1")

  }

  "Post /slots endpoint" should {
    "create a new slot" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newAdminUserInformation))
      slotService.createSlot("Meetup", dateTime)(newAdminUserInformation) shouldReturn Future(
            Right(slotWithId)
          )
      val body: JsValue = Json.parse("""
                                       |{
                                       |"slotType": "Meetup",
                                       |"dateTime":5656649365200
                                       |}
                            """.stripMargin)
      Post(s"/slots", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "return status code internal server error" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newAdminUserInformation))
      slotService.createSlot("Meetup", dateTime)(newAdminUserInformation) shouldReturn Future(
            Left(slotNotCreatedError)
          )
      val body: JsValue = Json.parse("""
                                       |{
                                       |"slotType": "Meetup",
                                       |"dateTime":5656649365200
                                       |}
                            """.stripMargin)
      Post(s"/slots", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "return status code unauthorized" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newEmployeeUserInformation))
      slotService.createSlot("Meetup", dateTime)(newEmployeeUserInformation) shouldReturn Future(
            Left(accessDeniedError)
          )
      val body: JsValue = Json.parse("""
                                       |{
                                       |"slotType": "Meetup",
                                       |"dateTime":5656649365200
                                       |}
                            """.stripMargin)
      Post(s"/slots", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.Unauthorized
      }
    }
  }

  "Put /slots/id " should {
    "update the slot based on the given information" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newAdminUserInformation))
      slotService.updateSlot(any[String], any[String], any[Long])(any[NewUserInformation]) shouldReturn Future(Right(Done))
      val body: JsValue = Json.parse("""
                                      |{
                                      |"newSlotType": "Meetup",
                                      |"newDateTime": 1235665656
                                      |}
                        """.stripMargin)
      Put(s"/slots/1234", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "return status code internal server error" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newAdminUserInformation))
      slotService.updateSlot("123", "Webinar", dateTime)(newAdminUserInformation) shouldReturn Future(Left(slotNotUpdatedError))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"newSlotType": "Meetup",
                                       |"newDateTime": 1125652
                                       |}
                        """.stripMargin)

      Put(s"/slots/1234", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }

    "return status code unauthorized" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(None)
      slotService.updateSlot("123", "Webinar", dateTime)(newEmployeeUserInformation) shouldReturn Future(Left(accessDeniedError))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"slotType": "Meetup",
                                       |"dateTime":5656649365200
                                       |}
                            """.stripMargin)
      Post(s"/slots/1234", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.Unauthorized
      }
    }
  }

  "Delete /slots/id" should {
    "delete the slot if that slot is not booked" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newAdminUserInformation))
      slotService.deleteSlot(any[String])(any[NewUserInformation]) shouldReturn Future(Right(Done))
      Delete(s"/slots/1122").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "return status code forbidden if the slot is booked" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newAdminUserInformation))
      slotService.deleteSlot(any[String])(any[NewUserInformation]) shouldReturn Future(Left(slotBookedError))

      Delete(s"/slots/1122").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.Forbidden
      }
    }

    "return status code not found if the slot does not exist" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newAdminUserInformation))
      slotService.deleteSlot(any[String])(any[NewUserInformation]) shouldReturn Future(Left(slotNotFoundError))

      Delete(s"/slots/1122").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return status code unauthorized" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newEmployeeUserInformation))
      slotService.deleteSlot(any[String])(any[NewUserInformation]) shouldReturn Future(Left(accessDeniedError))
      Delete(s"/slots/1122").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.Unauthorized
      }
    }
  }

  "Get /getFourMonths endpoint" should {

    "return slots" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      slotService.getSlotsInMonth shouldReturn future(Seq(getSlotsResponse))
      Get(s"/slots/getFourMonths").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Check Translate error for Access Denied " should {
    "return translate error" in new Setup {
      newSlotRoutes.translateError(SlotServiceError.AccessDenied) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Access Denied", None, List()))
    }
  }
}
