package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites }

case class UpcomingSessionsResponse(sessions: Seq[UpcomingSession])

object UpcomingSessionsResponse {

  implicit val UpcomingSessionResponseWrites: OWrites[UpcomingSessionsResponse] = Json.writes[UpcomingSessionsResponse]

  def fromDomain(sessions: Seq[UpcomingSession]): UpcomingSessionsResponse =
    UpcomingSessionsResponse(
      sessions
    )
}
