package com.knoldus.routes.session

import java.util.Date
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.session.SessionRequest
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.{ KeycloakDetails, NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.session.{ AddSlotRequest, DeclineSessionRequest, UpdateApproveSessionInfo }
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.session.CalendarService
import com.knoldus.services.session.CalendarService.CalendarServiceError
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{ JsValue, Json }

import scala.concurrent.Future

class CalendarRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {

    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: CalendarService = mock[CalendarService]
    val routes: Route = new CalendarRoutes(service, authorizationService).routes
    val dateTest: Date = new Date
    val sampleDate: Date = new Date(1569492774160L)
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val addSlotRequest: AddSlotRequest = AddSlotRequest("freeslot", sampleDate, isNotification = false)
    val id = "1"
    val error: CalendarServiceError = mock[CalendarServiceError]
    val pageNumber = 1
    val pageSize = 10
    val startDate = 1L
    val endDate = 2L
    val calendarRoutes = new CalendarRoutes(service, authorizationService)
    val testEmail = "test"
    val testName = "Testing"
    val dateTimeUtility = new DateTimeUtility
    val date = new Date(dateTimeUtility.toMillis(dateTimeUtility.localDateTimeIST.plusDays(1)))
    cache.remove("token")
    val validationDetails: KeycloakDetails = KeycloakDetails(testEmail, Admin)

    implicit val newUserInformation: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Admin
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
    val result: Future[UserToken] = cache.getOrLoad("token", _ => Future(UserToken("token", date, userInformation)))
    val withIdUser: WithId[UserInformation] = WithId(entity = userInformation, "1")

    val updateApproveSessionInfo: UpdateApproveSessionInfo =
      UpdateApproveSessionInfo(
        sampleDate,
        randomString,
        randomString,
        randomString,
        randomString,
        randomString,
        meetup = randomBoolean,
        brief = randomString,
        approved = randomBoolean,
        decline = randomBoolean,
        freeSlot = randomBoolean,
        notification = randomBoolean,
        randomString
      )
    val declineSessionRequest: DeclineSessionRequest = DeclineSessionRequest(approved = false, declined = true)

    val sessionRequest: SessionRequest = SessionRequest(
      approved = randomBoolean,
      category = randomString,
      date = dateTest,
      decline = randomBoolean,
      email = randomString,
      freeSlot = randomBoolean,
      meetup = randomBoolean,
      brief = randomString,
      notification = randomBoolean,
      recommendationId = randomString,
      subCategory = randomString,
      topic = randomString
    )

    val sessionRequestTest: SessionRequest = SessionRequest(
      approved = randomBoolean,
      category = randomString,
      date = dateTest,
      decline = randomBoolean,
      email = "",
      freeSlot = randomBoolean,
      meetup = randomBoolean,
      brief = randomString,
      notification = randomBoolean,
      recommendationId = randomString,
      subCategory = randomString,
      topic = randomString
    )
    val withId: WithId[SessionRequest] = WithId(sessionRequest, id)
    val withIdTest: WithId[SessionRequest] = WithId(sessionRequestTest, id)
  }

  "Delete /freeslot/id endpoint" should {

    "delete given slot " in new Setup {
      service.deleteSlot(any[String]) shouldReturn Future(Right(()))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Delete(s"/freeslot/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Delete /freeslot/id endpoint" should {

    "return error " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.deleteSlot(any[String]) shouldReturn Future(Left(error))
      Delete(s"/freeslot/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /freeslot endpoint" should {

    "return all free slots " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.getAllFreeSlots shouldReturn Future(Right(Seq(withId)))
      Get(s"/freeslot").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /freeslot endpoint" should {

    "return error " in new Setup {

      service.getAllFreeSlots shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/freeslot").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Post /freeslot endpoint" should {

    "should insert free slot " in new Setup {

      service.insertSlot(addSlotRequest) shouldReturn Future(Right(withId))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"slotName":"freeslot",
                                       |"date":1569492774160,
                                       |"isNotification":false
                                       |}
                            """.stripMargin)
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Post(s"/freeslot", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Post /freeslot endpoint" should {

    "return error " in new Setup {

      service.insertSlot(addSlotRequest) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"slotName":"freeslot",
                                       |"date":1569492774160,
                                       |"isNotification":false
                                       |}
                            """.stripMargin)
      Post(s"/freeslot", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /calender with no search keyword" should {

    "return all sessions " in new Setup {

      service.getAllSessionsForAdmin(any[Int], any[Int], None) shouldReturn Future(Right(Tuple2(Seq(withId), 1)))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/calendar?pageNumber=$pageNumber&pageSize=$pageSize")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get /calender with no search keyword" should {

    "return error " in new Setup {

      service.getAllSessionsForAdmin(any[Int], any[Int], None) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/calendar?pageNumber=$pageNumber&pageSize=$pageSize")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get /calender with search keyword" should {

    "return all sessions " in new Setup {

      service.getAllSessionsForAdmin(any[Int], any[Int], Some("Ab")) shouldReturn Future(Right(Tuple2(Seq(withId), 1)))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/calendar?pageNumber=$pageNumber&pageSize=$pageSize&email=Ab")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Get /calender with search keyword" should {

    "return error " in new Setup {

      service.getAllSessionsForAdmin(any[Int], any[Int], Some("Ab")) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/calendar?pageNumber=$pageNumber&pageSize=$pageSize&email=Ab")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get /calender session in month" should {

    "return sessions in given month " in new Setup {

      service.getSessionsInMonth(any[Long], any[Long]) shouldReturn Future(Right(Seq(withId)))
      Get(s"/calendar?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /calender session in month" should {

    "return error " in new Setup {

      service.getSessionsInMonth(any[Long], any[Long]) shouldReturn Future(Left(error))
      Get(s"/calendar?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /calender " should {

    "return pending sessions count " in new Setup {

      service.getPendingSessions shouldReturn Future(Right(1))
      Get(s"/calendar").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /calender " should {

    "return error " in new Setup {

      service.getPendingSessions shouldReturn Future(Left(error))
      Get("/calendar").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "POST /session/email endpoints" should {
    "send email to presenter" in new Setup {
      service.sendEmail(any[String]) shouldReturn Future(Right("1"))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"sessionId":"1"
                                       |}""".stripMargin)
      Post(s"/calendar/email", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "POST /session/email endpoints" should {
    "send email to all admins/superUser" in new Setup {
      service.sendEmail(any[String]) shouldReturn Future(Left(error))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"sessionId":"1"
                                       |}""".stripMargin)
      Post(s"/calendar/email", body).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /calendar/id " should {

    "return session of given Id " in new Setup {

      service.getSessionById(id) shouldReturn Future(Right(withId))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/calendar/get-session?id=$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /calendar/id " should {

    "return error " in new Setup {

      service.getSessionById(id) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      Get(s"/calendar/get-session?id=$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Post /calendar/id " should {

    "decline session " in new Setup {

      service.declineSession(id, declineSessionRequest) shouldReturn Future(Right(withIdTest))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"approved":false,
                                       |"declined":true
                                       |}""".stripMargin)

      Post(s"/calendar/decline-session?id=$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Post /calendar/id " should {

    "return error " in new Setup {

      service.declineSession(id, declineSessionRequest) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"approved":false,
                                       |"declined":true
                                       |}""".stripMargin)

      Post(s"/calendar/decline-session?id=$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Put /calendar/id " should {

    "update the given session " in new Setup {

      service.updateSessionForApprove(any[String], any[UpdateApproveSessionInfo]) shouldReturn Future(Right(()))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"date":1569492774160,
                                       |"sessionId":"",
                                       |"topic":"Free slot",
                                       |"email":"",
                                       |"category":"",
                                       |"subCategory":"",
                                       |"meetup":false,
                                       |"brief":"Major Topic",
                                       |"approved":false,
                                       |"decline":false,
                                       |"freeSlot":false,
                                       |"notification":false,
                                       |"recommendationId":""
                                       |}""".stripMargin)

      Put(s"/calendar/update-session-request?id=$id", body)
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "Put /calendar/id " should {

    "return error slot not found " in new Setup {

      service.updateSessionForApprove(any[String], any[UpdateApproveSessionInfo]) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"date":1569492774160,
                                       |"sessionId":"",
                                       |"topic":"Free slot",
                                       |"email":"",
                                       |"category":"",
                                       |"subCategory":"",
                                       |"meetup":false,
                                       |"brief":"Major Topic",
                                       |"approved":false,
                                       |"decline":false,
                                       |"freeSlot":false,
                                       |"notification":false,
                                       |"recommendationId":""
                                       |}""".stripMargin)

      Put(s"/calendar/update-session-request?id=$id", body)
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Put /calendar/id " should {

    "update date for given pending session " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      service.updateDateForPendingSession(any[String], sampleDate) shouldReturn Future(Right("123456"))
      val body: Date = sampleDate

      Put(s"/calendar/update-date?id=$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Put /calendar/id " should {

    "return error " in new Setup {

      service.updateDateForPendingSession(any[String], sampleDate) shouldReturn Future(Left(error))
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(newUserInformation))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      val body: Date = sampleDate

      Put(s"/calendar/update-date?id=$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Check Translate error for SlotNotFoundError " should {
    "return translate error" in new Setup {
      calendarRoutes.translateError(CalendarServiceError.SlotNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Slot Not Found", None, List()))
    }
  }

  "Check Translate error for SessionNotFoundError " should {
    "return translate error" in new Setup {
      calendarRoutes.translateError(CalendarServiceError.SessionNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Session Not Found", None, List()))
    }
  }

  "Check Translate error for AccessDenied " should {
    "return translate error" in new Setup {
      calendarRoutes.translateError(CalendarServiceError.AccessDenied) shouldBe
          Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Access Denied", None, List()))
    }
  }

}
