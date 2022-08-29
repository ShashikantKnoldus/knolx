package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.SessionRequest
import play.api.libs.json.{ Json, OWrites }

final case class AllSessionResponse(sessions: Seq[CalendarSession], count: Int, pages: Int)

object AllSessionResponse {
  implicit val AllSessionResponseWrites: OWrites[AllSessionResponse] = Json.writes[AllSessionResponse]

  def fromDomain(sessions: Seq[WithId[SessionRequest]], count: Int, pageSize: Int): AllSessionResponse = {
    val calenderSessions = sessions.map { sessionsInfo =>
      CalendarSession(
        id = sessionsInfo.id,
        date = sessionsInfo.entity.date,
        email = sessionsInfo.entity.email,
        topic = sessionsInfo.entity.topic,
        meetup = sessionsInfo.entity.meetup,
        brief = sessionsInfo.entity.brief,
        dateString = sessionsInfo.entity.date.toString,
        approved = sessionsInfo.entity.approved,
        decline = sessionsInfo.entity.decline,
        pending = !sessionsInfo.entity.approved && !sessionsInfo.entity.decline,
        freeSlot = false,
        contentAvailable = false,
        notification = false
      )

    }
    val pages = Math.ceil(count.toDouble / pageSize).toInt
    AllSessionResponse(calenderSessions, count, pages)
  }
}
