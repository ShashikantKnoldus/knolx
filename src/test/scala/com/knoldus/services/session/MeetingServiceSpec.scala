package com.knoldus.services.session

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{HttpExt, HttpsConnectionContext}
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.domain.session.{MeetingRequest, NewSession, WebexMeetingResponse}
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.session.PresenterDetails
import com.knoldus.{BaseSpec, RandomGenerators}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext

class MeetingServiceSpec extends BaseSpec {

  trait Setup {
    val token: String = conf.getConfig("webex").getString("access-token")
    val webexUrl: String = conf.getConfig("webex").getString("webexUrl")
    val newSessionDao: NewSessionDao = mock[NewSessionDao]
    val randomString: String = RandomGenerators.randomString()
    val webexMeetingResponse: WebexMeetingResponse = mock[WebexMeetingResponse]
    val service = new MeetingService(newSessionDao, conf)
    val http: HttpExt = mock[HttpExt]
    val rawJobResponse: JsObject = Json.obj()
    val meetingServiceRequest: MeetingRequest = mock[MeetingRequest]
    val dateLong: Long = 1659190333000L

    val presenterDetails: PresenterDetails =
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
    val withIdSession: WithId[NewSession] = WithId(entity = sessionInfo, "1")
    implicit val userInformationTest: NewUserInformation =
      NewUserInformation(randomString,
        randomString,
        Admin
      )

  }

  "WebexMeetingService" should {
    "give meeting response" in new Setup {
    when(newSessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(Option(withIdSession))
      when(
        http.singleRequest(
          any[HttpRequest],
          any[HttpsConnectionContext],
          any[ConnectionPoolSettings],
          any[LoggingAdapter]
        )
      ) thenReturn future(HttpResponse(status = StatusCodes.OK, entity = httpEntity(rawJobResponse)))
      whenReady(service.webexMeetingLink("62c2a4581fa2d068cd8a966f", meetingServiceRequest)(userInformationTest)) { result =>
       Thread.sleep(7000)
        assert(result.isRight)
      }
    }
  }


  "WebexMeetingService error" should {
    "give meeting response" in new Setup {
      when(newSessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(None)
      when(
        http.singleRequest(
          any[HttpRequest],
          any[HttpsConnectionContext],
          any[ConnectionPoolSettings],
          any[LoggingAdapter]
        )
      ) thenReturn future(HttpResponse(status = StatusCodes.OK, entity = httpEntity(rawJobResponse)))
      whenReady(service.webexMeetingLink("62c2a4581fa2d068cd8a966f", meetingServiceRequest)(userInformationTest)) { result =>
        Thread.sleep(7000)
        assert(!result.isRight)
      }
    }
  }







}
