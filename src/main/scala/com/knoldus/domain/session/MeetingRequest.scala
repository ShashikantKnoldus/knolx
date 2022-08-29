package com.knoldus.domain.session

import play.api.libs.json.{ Json, Reads }

case class MeetingRequest(date: String, meetingType: String, title: String)

object MeetingRequest {
  implicit val getMeetingRequestReads: Reads[MeetingRequest] = Json.reads[MeetingRequest]
}
