package com.knoldus.services.email

import akka.Done
import akka.actor.Scheduler
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.stream.Materializer
import com.knoldus.services.http.HttpClientService
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.{ Json, OWrites }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure

class NotificationService(val conf: Config, val http: HttpExt)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer,
  val logger: LoggingAdapter,
  val scheduler: Scheduler
) extends MailerService
    with HttpClientService
    with PlayJsonSupport {

  import NotificationService._

  private val mailConfig: Config = conf.getConfig("mail")
  private val sender = mailConfig.getString("sender")
  private val sendgridApiKey = mailConfig.getString("sendgrid-api-key")
  private val notificationServiceBaseUrl = mailConfig.getString("notification-service-url")

  private val emailUrl = "/notify/email/sendgrid"
  private val notificationEmailUrl = notificationServiceBaseUrl ++ emailUrl

  override def sendMessage(receivers: List[String], subject: String, content: String)(implicit
    ec: ExecutionContext
  ): Future[Done] = {

    val notification = Notification(
      recipients = receivers,
      cc = Nil,
      bcc = Nil,
      sender = sender,
      htmlBody = content,
      htmlTitle = subject
    )

    val httpHeader = RawHeader("SENDGRID_API_KEY", sendgridApiKey)
    (for {
      entity <- Marshal(notification).to[MessageEntity]
      _ <- makeHttpRequest(
        HttpRequest(POST, notificationEmailUrl, entity = entity).addHeader(httpHeader),
        List(StatusCodes.OK, StatusCodes.Accepted)
      )
    } yield Done).andThen {
      case Failure(exception) => logger.error("Failed to send email", exception)
    }

  }

  override def sendMessage(receiver: String, subject: String, content: String)(implicit
    ec: ExecutionContext
  ): Future[Done] = sendMessage(List(receiver), subject, content)

}

object NotificationService {

  case class Notification(
    recipients: List[String],
    cc: List[String],
    bcc: List[String],
    sender: String,
    htmlBody: String,
    htmlTitle: String
  )

  implicit val notificationWrites: OWrites[Notification] = Json.writes[Notification]
}
