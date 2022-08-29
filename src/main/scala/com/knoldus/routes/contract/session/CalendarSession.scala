package com.knoldus.routes.contract.session

import java.util.Date

import play.api.libs.json.{ Json, OWrites }

final case class CalendarSession(
  id: String,
  date: Date,
  email: String,
  topic: String,
  meetup: Boolean,
  brief: String,
  dateString: String,
  approved: Boolean,
  decline: Boolean,
  pending: Boolean,
  freeSlot: Boolean,
  contentAvailable: Boolean,
  notification: Boolean
)

object CalendarSession {
  implicit val CalendarSessionWrites: OWrites[CalendarSession] = Json.writes[CalendarSession]
}
