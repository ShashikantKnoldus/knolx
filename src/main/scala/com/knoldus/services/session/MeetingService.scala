package com.knoldus.services.session

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.domain.session.{MeetingRequest, MeetingResponse, WebexMeetingResponse}
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.services.session.MeetingService.MeetingServiceError
import com.typesafe.config.Config
import play.api.libs.json.Json

import java.text.SimpleDateFormat
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MeetingService(newSessionDao: NewSessionDao, conf: Config)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter,
  val system: ActorSystem
) {

  val token: String = conf.getConfig("webex").getString("access-token")
  val webexUrl: String = conf.getConfig("webex").getString("webexUrl")

  /** **
    *
    * @param sessionId
    * @param meetingRequest
    * @param authorityUser
    * @return This method returns the webex meeting data coming fromm the webex API
    */

  def meetingLink(sessionId: String, meetingRequest: MeetingRequest)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[MeetingServiceError, MeetingResponse]] =
    if (meetingRequest.meetingType == "webex")
      webexMeetingLink(sessionId, meetingRequest)
    else if (meetingRequest.meetingType == "teams")
      teamsMeetingLink(sessionId, meetingRequest)
    else Future.successful(Left(MeetingServiceError.InvalidMeetingType))

  def webexMeetingLink(sessionId: String, meetingRequest: MeetingRequest)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[MeetingServiceError, MeetingResponse]] =
    if (authorityUser.isAdmin)
      webexMeetingRequest(sessionId).flatMap {
        case Right(meetReq) =>
          Http().singleRequest(webexRequest(meetReq)).flatMap { webexResponse =>
            webexResponse.entity.toStrict(300.millis).map(_.data).map(_.utf8String).map { webexResponseData =>
              logger.info(webexResponseData)
              Try {
                Json.parse(webexResponseData).as[WebexMeetingResponse]
              } match {
                case Success(webexResp: WebexMeetingResponse) =>
                  logger.info(webexResp.toString)
                  Right(
                    MeetingResponse(
                      meetingRequest.meetingType,
                      webexResp.start,
                      webexResp.webLink,
                      webexResp.id,
                      webexResp.title
                    )
                  )
                case Failure(err) =>
                  Left(MeetingServiceError.DataNotFound)
              }
            }
          }
        case Left(error) => Future.successful(Left(error))
      }
    else
      Future.successful(Left(MeetingServiceError.AccessDenied))

  def teamsMeetingLink(sessionId: String, meetingRequest: MeetingRequest)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[MeetingServiceError, MeetingResponse]] =
    if (authorityUser.isAdmin)
      Future.successful(Right(MeetingResponse(meetingRequest.meetingType, "1659703500000", "link", "id", "my meet")))
    else
      Future.successful(Left(MeetingServiceError.AccessDenied))

  private def webexRequest(meetingReq: String): HttpRequest =
    HttpRequest(
      method = HttpMethods.POST,
      uri = webexUrl,
      entity = HttpEntity(ContentTypes.`application/json`, meetingReq)
    ).withHeaders(Authorization(OAuth2BearerToken(token)))

  private def webexMeetingRequest(id: String): Future[Either[MeetingServiceError, String]] =
    newSessionDao.get(id).flatMap {
      case Some(data) =>
        val sessionData = data.entity
        val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val request =
          s"""
             |{
             |    "title": "${sessionData.topic}",
             |    "agenda": "${sessionData.sessionDescription}",
             |    "password": "Knoldus",
             |   "start": "${df.format(sessionData.dateTime)}",
             |    "end": "${df.format(sessionData.dateTime + 2700000)}",
             |    "timezone": "Asia/Kolkata",
             |    "enabledAutoRecordMeeting": false,
             |    "allowAnyUserToBeCoHost": false,
             |    "invitees": [
             |        {
             |            "email": "xyz@knoldus.com",
             |            "displayName": "xyz",
             |            "coHost": false
             |        }
             |    ]
             |}
             |""".stripMargin
        Future.successful(Right(request))

      case None => Future.successful(Left(MeetingServiceError.SessionNotFound))
    }

}

object MeetingService {

  sealed trait MeetingServiceError

  object MeetingServiceError {

    case object AccessDenied extends MeetingServiceError

    case object SessionNotFound extends MeetingServiceError

    case object DataNotFound extends MeetingServiceError

    case object InvalidMeetingType extends MeetingServiceError

  }

}
