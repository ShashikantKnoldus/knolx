package com.knoldus.routes.contract.session

import java.util.Date
import play.api.libs.json.{ Json, Reads }

final case class UpdateSessionRequest(
  session: Option[String] = None,
  date: Option[Date] = None,
  category: Option[String] = None,
  subCategory: Option[String] = None,
  feedbackFormId: Option[String] = None,
  topic: Option[String] = None,
  brief: Option[String] = None,
  feedbackExpirationDays: Option[Int] = None,
  youtubeURL: Option[String] = None,
  slideShareURL: Option[String] = None,
  cancelled: Option[Boolean] = None,
  meetup: Option[Boolean] = None
)

final case class UpdateUserSession(
  topic: Option[String] = None,
  sessionDescription: Option[String] = None,
  slideURL: Option[String] = None,
  sessionTag: Option[List[String]] = None
)

object UpdateSessionRequest {
  implicit val UpdateSessionRequestReads: Reads[UpdateSessionRequest] = Json.reads[UpdateSessionRequest]
}

object UpdateUserSession {
  implicit val updateUserSessionReads: Reads[UpdateUserSession] = Json.reads[UpdateUserSession]
}
