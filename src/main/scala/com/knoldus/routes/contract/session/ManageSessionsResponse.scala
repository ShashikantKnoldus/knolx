package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.NewSession
import play.api.libs.json.{ Json, OWrites }

final case class ManageSessionsResponse(sessions: Seq[GetSessionByIdNewResponse], count: Int, pages: Int)

object ManageSessionsResponse {

  implicit val requestedSessionsResponseWrites: OWrites[ManageSessionsResponse] =
    Json.writes[ManageSessionsResponse]

  def fromDomain(sessions: Seq[WithId[NewSession]], count: Int, pageSize: Int): ManageSessionsResponse = {
    val requestedSessions = sessions.map { session =>
      GetSessionByIdNewResponse.fromDomain(session)
    }
    val pages = Math.ceil(count.toDouble / pageSize).toInt
    ManageSessionsResponse(requestedSessions, count, pages)
  }
}
