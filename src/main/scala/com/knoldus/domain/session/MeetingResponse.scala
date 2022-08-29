package com.knoldus.domain.session

import play.api.libs.json.{ Json, OWrites, Reads }

case class MeetingResponse(meetingType: String, startTime: String, meetUrl: String, meetingId: String, title: String)

object MeetingResponse {
  implicit val getMeetingResponseReads: Reads[MeetingResponse] = Json.reads[MeetingResponse]
  implicit val getMeetingResponseWrite: OWrites[MeetingResponse] = Json.writes[MeetingResponse]
}

case class WebexMeetingResponse(id: String, title: String, start: String, webLink: String)

object WebexMeetingResponse {
  implicit val getMeetingResponseReads: Reads[WebexMeetingResponse] = Json.reads[WebexMeetingResponse]
  implicit val getMeetingResponseWrite: OWrites[WebexMeetingResponse] = Json.writes[WebexMeetingResponse]
}

case class TeamsMeetingResponse(
  startDateTime: String,
  endDateTime: String,
  id: String,
  joinWebUrl: String,
  subject: String
)

object TeamsMeetingResponse {
  implicit val getMeetingResponseRead: Reads[TeamsMeetingResponse] = Json.reads[TeamsMeetingResponse]
  implicit val getMeetingResponseWrite: OWrites[TeamsMeetingResponse] = Json.writes[TeamsMeetingResponse]
}
