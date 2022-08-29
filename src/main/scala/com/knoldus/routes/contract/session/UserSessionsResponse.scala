package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.NewSession
import play.api.libs.json.{ Json, OWrites }

final case class UserSessionsResponse(sessions: Seq[GetSessionByIdNewResponse], count: Int)

object UserSessionsResponse {

  implicit val userSessionsResponseWrites: OWrites[UserSessionsResponse] =
    Json.writes[UserSessionsResponse]

  def fromDomain(sessions: Seq[WithId[NewSession]], count: Int): UserSessionsResponse = {
    val upcomingSessions = sessions.map { session =>
      GetSessionByIdNewResponse.fromDomain(session)
    }
    UserSessionsResponse(
      upcomingSessions,
      count
    )
  }
}
