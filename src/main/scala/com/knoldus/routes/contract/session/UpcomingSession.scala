package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites }

import java.util.Date

final case class UpcomingSession(
  label: String,
  category: String,
  topic: String,
  email: String,
  name: String,
  date: Date
)

object UpcomingSession {
  implicit val KnolxSessionWrites: OWrites[UpcomingSession] = Json.writes[UpcomingSession]
}
