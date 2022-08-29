package com.knoldus.routes.contract.session

import play.api.libs.json.{ JsValue, Json, OWrites, Writes }

final case class KnolxSession(
  id: String,
  presenterDetail: PresenterDetails,
  coPresenterDetails: Option[PresenterDetails],
  dateTime: Long,
  sessionDurationInMins: Long,
  topic: String,
  category: String,
  subCategory: String,
  feedbackFormId: Option[String],
  feedbackExpriationDate: Long,
  sessionType: String,
  sessionState: String,
  sessionDescription: String,
  contentAvailable: Boolean,
  content: ContentInformation,
  slidesApprovedBy: Option[String],
  sessionApprovedBy: Option[String]
)

object KnolxSession {

  implicit object ContentStatusWrites extends Writes[ContentStatus] {

    override def writes(o: ContentStatus): JsValue =
      o match {
        case ContentStatus.Available => Json.toJson("Available")
        case ContentStatus.NotAvailable => Json.toJson("NotAvailable")
        case ContentStatus.Cancelled => Json.toJson("Cancelled")
      }
  }

  implicit val ContentInformationWrites: OWrites[ContentInformation] = Json.writes[ContentInformation]

  implicit val KnolxSessionWrites: OWrites[KnolxSession] = Json.writes[KnolxSession]
}
