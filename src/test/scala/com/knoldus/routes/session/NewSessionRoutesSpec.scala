package com.knoldus.routes.session

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.dao.slot.SlotDao
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.session.NewSession
import com.knoldus.domain.slot.Slot
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.{ NewUserInformation, UserInformation, UserToken }
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.session._
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.keyCloak.KeycloakIAMService
import com.knoldus.services.scheduler.SchedulerService
import com.knoldus.services.scheduler.SessionScheduler.ScheduleSession
import com.knoldus.services.session.NewSessionService
import com.knoldus.services.session.NewSessionService.NewSessionServiceError
import com.knoldus.services.usermanagement.AuthorizationService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{ JsValue, Json }

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

class NewSessionRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val iamService: KeycloakIAMService = mock[KeycloakIAMService]
    val userDao: UserDao = mock[UserDao]
    val authorizationService = new AuthorizationService(cache, clientCache, userDao, iamService)
    val service: NewSessionService = mock[NewSessionService]
    val slotDao: SlotDao = mock[SlotDao]
    val newSessionDao: NewSessionDao = mock[NewSessionDao]
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val newSessionRoutes = new NewSessionRoutes(service, authorizationService)
    val routes: Route = new NewSessionRoutes(service, authorizationService).routes
    val id = "1"
    val sessionID = "12"
    val pageNumber = 1
    val pageSize = 10
    val filter = "filter"
    val category = ""
    val dateTest: Date = new Date
    val sampleDate: Date = new Date(1569492774160L)
    val dateLong: Long = 1216534216
    val longDate = 1234
    val error: NewSessionServiceError = mock[NewSessionServiceError]
    val newSesssionRoutes = new NewSessionRoutes(service, authorizationService)
    val testEmail = "test"
    val testName = "Testing"
    val dateTimeUtility = new DateTimeUtility
    val date = new Date(dateTimeUtility.toMillis(dateTimeUtility.localDateTimeIST.plusDays(1)))
    val validationDetails: NewUserInformation = NewUserInformation(testEmail, testName, Admin)
    val schedulerService: SchedulerService = mock[SchedulerService]

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

    implicit val newUserInformation: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Admin
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

    val sessionInfo: NewSession =
      NewSession(
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
        sessionState = randomString,
        sessionDescription = randomString,
        youtubeURL = Some(randomString),
        slideShareURL = Some(randomString),
        slideURL = Some(randomString),
        slidesApprovedBy =  Some(randomString),
        sessionApprovedBy =  Some(randomString),
        sessionTag = List(randomString),
        remarks = None
      )

    val sessionInfo1: NewSession = NewSession(
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
      sessionState = randomString,
      sessionDescription = randomString,
      youtubeURL = Some(randomString),
      slideShareURL = Some(randomString),
      slideURL = Some(randomString),
      sessionApprovedBy = Some(randomString),
      slidesApprovedBy =  Some(randomString),
      sessionTag = List(randomString),
      remarks = None
    )
    val tag: String = randomString
    val newSessionWithId: WithId[NewSession] = WithId(sessionInfo, "1")
    val newSessionWithNewId: WithId[NewSession] = WithId(sessionInfo1, "1")
    val withId: WithId[NewSession] = WithId(entity = sessionInfo, "1")
    val withIdUser: WithId[UserInformation] = WithId(entity = userInformation, "1")
    val withIdSeq: Seq[WithId[NewSession]] = Seq(withId)
    val withIdUpcomingSeq: Seq[(WithId[NewSession], String)] = Seq((withId, randomString))
    val sessionResponse: (Seq[WithId[NewSession]], Int) = Tuple2(Seq(WithId(entity = sessionInfo, "1")), 1)
    val slot: Slot = Slot("Knolx", 1655103142, bookable = true, "Admin", 121212212, None, 45)
    val slotWithId: WithId[Slot] = WithId(slot, "1")

  }
  "Get /sessions/manage endpoint" should {

    "return sessions pending for approval when search is defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getRequestedSessions(1,10,Some("something"))(userInformationAdmin)) thenReturn future(
        Right((Seq(withId),1))
      )
      Get(s"/sessions/manage?pageNumber=$pageNumber&pageSize=$pageSize&filter=requested&search=something").addHeader(RawHeader("Authorization", "Bearer Token"))check {
        status shouldBe StatusCodes.OK
      }
    }
    "return sessions pending for approval when search is not defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getRequestedSessions(1,10)(userInformationAdmin)) thenReturn future(
        Right((Seq(withId),1))
      )
      Get(s"/sessions/manage?pageNumber=$pageNumber&pageSize=$pageSize&filter=requested").addHeader(RawHeader("Authorization", "Bearer Token"))check {
        status shouldBe StatusCodes.OK
      }
    }
    "return approved upcoming sessions when search is defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getApprovedSessions(1,10,Some("something"))(userInformationAdmin)) thenReturn future(
        Right((Seq(withId),1))
      )
      Get(s"/sessions/manage?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming&search=something").addHeader(RawHeader("Authorization", "Bearer Token"))check {
        status shouldBe StatusCodes.OK
      }
    }
    "return approved upcoming sessions when search is not defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getApprovedSessions(1,10)(userInformationAdmin)) thenReturn future(
        Right((Seq(withId),1))
      )
      Get(s"/sessions/manage?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming").addHeader(RawHeader("Authorization", "Bearer Token"))check {
        status shouldBe StatusCodes.OK
      }
    }
    "return 401 error for filter requested" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationEmployee))
      when(service.getRequestedSessions(1,10)(userInformationEmployee)) thenReturn future(
        Left(NewSessionServiceError.AccessDenied)
      )
      Get(s"/sessions/manage?pageNumber=$pageNumber&pageSize=$pageSize&filter=requested").addHeader(RawHeader("Authorization", "Bearer Token"))check {
        status shouldBe StatusCodes.Unauthorized
      }
    }
    "return 401 error for filter upcoming" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationEmployee))
      when(service.getApprovedSessions(1,10)(userInformationEmployee)) thenReturn future(
        Left(NewSessionServiceError.AccessDenied)
      )
      Get(s"/sessions/manage?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming").addHeader(RawHeader("Authorization", "Bearer Token"))check {
        status shouldBe StatusCodes.Unauthorized
      }
    }
  }
  "Get /sessions/tags endpoint" should {

    "return matching tags" in new Setup {
      when(service.fetchSessionTags(any[String])) thenReturn future(
        Right(Seq(tag))
      )

      Get(s"/sessions/tags/$tag").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error" in new Setup {
      when(service.fetchSessionTags(any[String])) thenReturn future(
        Left(NewSessionServiceError.NotFound)
      )

      Get(s"/sessions/tags/$tag").check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  "Get /sessions/ endpoint" should {

    "return upcoming sessions when search is defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getUpcomingSessions(any[Int], any[Int], Some(any[String]))) thenReturn future(
            Right(Tuple2(withIdSeq, 1))
          )

      Get(s"/sessions?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming&search=$testEmail").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return past sessions when search is defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getPastSessions(any[Int], any[Int], Some(any[String]))) thenReturn future(
            Right(Tuple2(withIdSeq, 1))
          )

      Get(s"/sessions?pageNumber=$pageNumber&pageSize=$pageSize&filter=past&search=$testEmail").check {
        status shouldBe StatusCodes.OK
      }
    }

    "return upcoming sessions when search is not defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getUpcomingSessions(any[Int], any[Int], any[Option[String]])) thenReturn future(
            Right(Tuple2(withIdSeq, 1))
          )
      Get(s"/sessions?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming").check {
        status shouldBe StatusCodes.OK
      }
    }
    "return past sessions when search is not defined" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getPastSessions(any[Int], any[Int], any[Option[String]])) thenReturn future(
            Right(Tuple2(withIdSeq, 1))
          )

      Get(s"/sessions?pageNumber=$pageNumber&pageSize=$pageSize&filter=past").check {
        status shouldBe StatusCodes.OK
      }
    }
    "return error" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getUpcomingSessions(any[Int], any[Int], Some(any[String]))) thenReturn future(Left(error))

      Get(s"/sessions?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming&search=$testEmail").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }
  "Get /sessions/my endpoint" should {

    "return user upcoming sessions " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getUserSessions(any[Int], any[Int], any[String])(any[NewUserInformation])) thenReturn future(
            Right(Tuple2(withIdSeq, 1))
          )

      Get(s"/sessions/my?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return user past sessions " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getUserSessions(any[Int], any[Int], any[String])(any[NewUserInformation])) thenReturn future(
            Right(Tuple2(withIdSeq, 1))
          )

      Get(s"/sessions/my?pageNumber=$pageNumber&pageSize=$pageSize&filter=past")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.OK
        }
    }

    "return error " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.getUserSessions(any[Int], any[Int], any[String])(any[NewUserInformation])) thenThrow classOf[
            NullPointerException
          ]

      Get(s"/sessions/my?pageNumber=$pageNumber&pageSize=$pageSize&filter=upcoming")
        .addHeader(RawHeader("Authorization", "Bearer Token"))
        .check {
          status shouldBe StatusCodes.InternalServerError
        }
    }
  }

  "Get /sessions/bookSession endpoint" should {

    "book session " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.bookSession(any[BookSessionUserRequest])(any[NewUserInformation])) thenReturn future(
            Right(newSessionWithId)
          )
      when(slotDao.get(any[String])(any[ExecutionContext])) thenReturn future(Option(slotWithId))
      when(slotDao.update(any[String], any[Slot => Slot].apply)(any[ExecutionContext])) thenReturn future(
            Option(slotWithId)
          )
      when(newSessionDao.create(any[NewSession])(any[ExecutionContext])) thenReturn future("id")

      val body: JsValue = Json.parse("""
                                       |{
                                       |    "slotId": "62a71eca53d9bfcab758eb65",
                                       |    "coPresenterDetail": {
                                       |        "fullName": "Someone",
                                       |        "email": "Something"
                                       |    },
                                       |    "topic": "topic",
                                       |    "category": "category",
                                       |    "subCategory": "subCategory",
                                       |    "sessionType": "Knolx",
                                       |    "sessionDescription": "Test session description"
                                       |}
                                       |""".stripMargin)
      Post(s"/sessions/bookSession", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "mandatory field not found error " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(service.bookSession(any[BookSessionUserRequest])(any[NewUserInformation])) thenReturn future(
        Left(NewSessionServiceError.MandatoryFieldsNotFound)
      )
      when(slotDao.get(any[String])(any[ExecutionContext])) thenReturn future(Option(slotWithId))
      when(slotDao.update(any[String], any[Slot => Slot].apply)(any[ExecutionContext])) thenReturn future(
        Option(slotWithId)
      )
      when(newSessionDao.create(any[NewSession])(any[ExecutionContext])) thenReturn future("id")

      val body: JsValue = Json.parse("""
                                       |{
                                       |    "slotId": "62a71eca53d9bfcab758eb65",
                                       |    "coPresenterDetail": {
                                       |        "fullName": "Someone",
                                       |        "email": "Something"
                                       |    },
                                       |    "topic": "",
                                       |    "category": "category",
                                       |    "subCategory": "subCategory",
                                       |    "sessionType": "Knolx",
                                       |    "sessionDescription": "Test session description"
                                       |}
                                       |""".stripMargin)
      Post(s"/sessions/bookSession", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return error " in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      service.bookSession(any[BookSessionUserRequest])(userInformationAdmin) shouldReturn Future(Left(error))
      when(slotDao.get(any[String])(any[ExecutionContext])) thenReturn future(Option(slotWithId))
      when(slotDao.update(any[String], any[Slot => Slot].apply)(any[ExecutionContext])) thenReturn future(
            Option(slotWithId)
          )
      when(newSessionDao.create(any[NewSession])(any[ExecutionContext])) thenReturn future("id")

      val body: JsValue = Json.parse("""
                                       |{
                                       |    "slotId": "62a71eca53d9bfcab758eb65",
                                       |    "coPresenterDetail": {
                                       |        "fullName": "Someone",
                                       |        "email": "Something"
                                       |    },
                                       |    "topic": "topic",
                                       |    "category": "category",
                                       |    "subCategory": "subCategory",
                                       |    "sessionType": "Knolx",
                                       |    "sessionDescription": "Test session description"
                                       |}
                                       |""".stripMargin)
      Post(s"/sessions/bookSession", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Put sessions/update-session endpoint" should {

    "return updated session" in new Setup {
      when(
        service.updateNewSessions(
          "1",
          Some(presenterDetails),
          Some(dateLong),
          Some("Topic"),
          Some("Database"),
          Some("Mysql"),
          Some("15"),
          Some("Knolx"),
          Some("sessionDescription")
        )(newUserInformation)
      ) thenReturn future(Right(Done))
      val body: JsValue = Json.parse("""
                                       |{
                                       |"coPresenterDetails":{
                                       |    "fullName": "Average Joe",
                                       |    "email":"average.joe@knoldus.com"
                                       |},
                                       |  "dateTime": 1216534216,
                                       |  "topic": "Topic",
                                       |  "category": "Database",
                                       |  "subCategory": "Mysql",
                                       |  "feedbackFormId": "15",
                                       |  "sessionType": "Knolx",
                                       |  "sessionDescription": "sessionDescription"
                                       |}
                            """.stripMargin)

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(validationDetails))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withId))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      Put(s"/sessions/update-session/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Put sessions/update-session endpoint" should {

    "return error" in new Setup {
      service.updateNewSessions(
        "1",
        Some(presenterDetails),
        Some(dateLong),
        Some("Topic"),
        Some("Database"),
        Some("Mysql"),
        Some("15"),
        Some("Knolx"),
        Some("sessionDescription")
      )(newUserInformation) shouldReturn Future(Left(error))
      val body: JsValue = Json.parse("""
                                    {
                                       |"coPresenterDetails":{
                                       |    "fullName": "Average Joe",
                                       |    "email":"average.joe@knoldus.com"
                                       |},
                                       |  "dateTime": 1216534216,
                                       |  "topic": "Topic",
                                       |  "category": "Database",
                                       |  "subCategory": "Mysql",
                                       |  "feedbackFormId": "15",
                                       |  "sessionType": "Knolx",
                                       |  "sessionDescription": "sessionDescription"
                                       |}
                            """.stripMargin)
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(validationDetails))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn Future(None)
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(None))
      Put(s"/sessions/update-session/id=$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /getSession endpoint" should {

    "return session" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn future(
            Some(newSessionWithNewId)
          )
      service.getNewSession(any[String])(userInformationAdmin) shouldReturn future(Right(newSessionWithNewId))
      Get(s"/getSession/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /getSession endpoint" should {

    "return error" in new Setup {
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(userInformationAdmin))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn future(Some(newSessionWithId))
      service.getNewSession("1")(userInformationAdmin) shouldReturn future(Left(error))
      Get(s"/getSession/$id").addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Update the session for user" should {

    "return the session with the updated details" in new Setup {
      when(service.updateUserSession(
        "1",
        Some("SCALA with Akka"),
        Some("Let's learn scala with akka actors"),
        Some("abc@scala_akka.com")
      )(newUserInformation) ) thenReturn future(Right(Done))
      val body: JsValue = Json.parse("""
                                       |{
                                       |  "topic": "SCALA with Akka",
                                       |  "sessionDescription": "Let's learn scala with akka actors",
                                       |  "slideURL": "abc@scala_akka.com"
                                       |}
                            """.stripMargin)

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(validationDetails))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withId))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      Put(s"/user/session/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
    "return the session with the single updated detail" in new Setup {
      when(service.updateUserSession(
        "1",
        Some("SCALA"),
        Some("Let's learn scala with akka actors"),
        None      )(newUserInformation) ) thenReturn future(Right(Done))
      val body: JsValue = Json.parse("""
                                       |{
                                       |  "topic": "SCALA",
                                       |  "sessionDescription": "Let's learn scala with akka actors"
                                       |}
                            """.stripMargin)

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(validationDetails))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withId))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      Put(s"/user/session/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }
    "return the session with the two updated detail" in new Setup {
      when(service.updateUserSession(
        "1",
        Some("SCALA"),
        None,
        None      )(newUserInformation) ) thenReturn future(Right(Done))
      val body: JsValue = Json.parse("""
                                       |{
                                       |  "topic": "SCALA"
                                       |}
                            """.stripMargin)

      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(validationDetails))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withId))
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      Put(s"/user/session/$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.OK
      }
    }

    "return error" in new Setup {
      service.updateUserSession(
        "1",
        Some("SCALA with Akka"),
        Some("Let's learn scala with akka actors"),
        Some("abc@scala_akka.com")
      )(newUserInformation) shouldReturn Future(Left(error))
      val body: JsValue = Json.parse("""
                                       |{
                                       |  "topic": "SCALA with Akka",
                                       |  "sessionDescription": "Let's learn scala with akka actors",
                                       |  "slideURL": "abc@scala_akka.com"
                                       |}
                            """.stripMargin)
      when(iamService.validateTokenAndGetDetails(any[String])) thenReturn future(Some(validationDetails))
      when(userDao.get(any[Filter])(any[concurrent.ExecutionContext])) thenReturn Future(Some(withIdUser))
      when(newSessionDao.get(any[String])(any[concurrent.ExecutionContext])) thenReturn Future(None)
      when(newSessionDao.update(any[String], any[NewSession => NewSession].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(None))
      Put(s"/update/session/id=$id", body).addHeader(RawHeader("Authorization", "Bearer Token")).check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  "Check Translate error for Access Denied " should {
    "return translate error" in new Setup {
      newSessionRoutes.translateError(NewSessionServiceError.AccessDenied) shouldBe
        Tuple2(StatusCodes.Unauthorized, ErrorResponse(401, "Access Denied", None, List()))
    }
  }
  "Check Translate error for InternalServerError " should {
    "return translate error" in new Setup {
      newSessionRoutes.translateError(NewSessionServiceError.InternalServerError) shouldBe
        Tuple2(StatusCodes.InternalServerError, ErrorResponse(500, "Internal Server Error", None, List()))
    }
  }
  "Check Translate error for Slot Not Found " should {
    "return translate error" in new Setup {
      newSessionRoutes.translateError(NewSessionServiceError.InvalidSlotId("1")) shouldBe
        Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Invalid slot id provided: 1", None, List()))
    }
  }
  "Check Translate error for Slot Not Available " should {
    "return translate error" in new Setup {
      newSessionRoutes.translateError(NewSessionServiceError.SlotNotAvailable) shouldBe
        Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Slot Not Available", None, List()))
    }
  }
  "Check Translate error for Filter Not Found " should {
    "return translate error" in new Setup {
      newSessionRoutes.translateError(NewSessionServiceError.FilterNotFound) shouldBe
        Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Request is missing required query parameter 'filter'", None, List()))
    }
  }
  "Check Translate error for Not Found " should {
    "return translate error" in new Setup {
      newSessionRoutes.translateError(NewSessionServiceError.NotFound) shouldBe
        Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Cannot Fetch Tags", None, List()))
    }
  }
  "Check Translate error for Mandatory Field Found " should {
    "return translate error" in new Setup {
      newSessionRoutes.translateError(NewSessionServiceError.MandatoryFieldsNotFound) shouldBe
        Tuple2(StatusCodes.NotFound, ErrorResponse(404, "Mandatory fields not given in the request", None, List()))
    }
  }

}
