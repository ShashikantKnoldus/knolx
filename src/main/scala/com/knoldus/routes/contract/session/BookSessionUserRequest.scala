package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, Reads }

case class BookSessionUserRequest(
  slotId: String,
  topic: String,
  category: String,
  subCategory: String,
  sessionType: String,
  sessionDescription: String,
  coPresenterDetail: Option[PresenterDetails],
  feedbackFormId: Option[String]
)

object BookSessionUserRequest {
  implicit val BookSessionUserRequestWrites: Reads[BookSessionUserRequest] = Json.reads[BookSessionUserRequest]
}
