package com.knoldus.services.mailservice

import akka.Done
import akka.actor.ExtendedActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.{ HttpExt, HttpsConnectionContext }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.knoldus.BaseSpec
import com.knoldus.services.email.{ MailerService, NotificationService }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{ JsObject, Json }

class MailerServiceSpec extends BaseSpec {
  val http: HttpExt = mock[HttpExt]
  val mailService: MailerService = new NotificationService(conf, http)
  val mailList: List[String] = List("user1", "user2")
  val extendedSystem: ExtendedActorSystem = mock[ExtendedActorSystem]
  val rawJobResponse: JsObject = Json.obj()

  "NotificationService/sendMessage" should {
    "send email to user" in {
      when(
        http.singleRequest(
          any[HttpRequest],
          any[HttpsConnectionContext],
          any[ConnectionPoolSettings],
          any[LoggingAdapter]
        )
      ) thenReturn future(HttpResponse(status = StatusCodes.OK, entity = httpEntity(rawJobResponse)))
      when(http.system) thenReturn extendedSystem
      whenReady(mailService.sendMessage("receiver", "subject", "content")) { result =>
        assert(result == Done)
      }
    }

    "send email to multiple user" in {
      when(
        http.singleRequest(
          any[HttpRequest],
          any[HttpsConnectionContext],
          any[ConnectionPoolSettings],
          any[LoggingAdapter]
        )
      ) thenReturn future(HttpResponse(status = StatusCodes.OK, entity = httpEntity(rawJobResponse)))
      when(http.system) thenReturn extendedSystem
      whenReady(mailService.sendMessage(mailList, "subject", "content")) { result =>
        assert(result == Done)
      }
    }
  }
}
