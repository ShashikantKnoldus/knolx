package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites, Reads }

final case class UpdateSessionUserRequest(
  coPresenterDetails: Option[PresenterDetails] = None,
  dateTime: Option[Long] = None,
  topic: Option[String] = None,
  category: Option[String] = None,
  subCategory: Option[String] = None,
  feedbackFormId: Option[String] = None,
  sessionType: Option[String] = None,
  sessionDescription: Option[String] = None
)

object UpdateSessionUserRequest {
  implicit val UpdateSessionUserRequestReads: Reads[UpdateSessionUserRequest] = Json.reads[UpdateSessionUserRequest]
  implicit val UpdateSessionUserRequestWrites: OWrites[UpdateSessionUserRequest] = Json.writes[UpdateSessionUserRequest]
}
