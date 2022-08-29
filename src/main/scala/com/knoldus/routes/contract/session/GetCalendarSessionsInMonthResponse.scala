package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.SessionRequest
import play.api.libs.json.{ Json, OWrites }

final case class GetCalendarSessionsInMonthResponse(sessions: Seq[CalendarSession])

object GetCalendarSessionsInMonthResponse {

  implicit val GetCalendarSessionsInMonthResponseWrites: OWrites[GetCalendarSessionsInMonthResponse] =
    Json.writes[GetCalendarSessionsInMonthResponse]

  def fromDomain(sessions: Seq[WithId[SessionRequest]]): GetCalendarSessionsInMonthResponse = {
    val knolxSessions = sessions.map { sessionInfo =>
      CalendarSession(
        sessionInfo.id,
        sessionInfo.entity.date,
        sessionInfo.entity.email,
        sessionInfo.entity.topic,
        sessionInfo.entity.meetup,
        sessionInfo.entity.brief,
        sessionInfo.entity.date.toString,
        sessionInfo.entity.approved,
        sessionInfo.entity.decline,
        !sessionInfo.entity.notification,
        sessionInfo.entity.freeSlot,
        contentAvailable = false,
        sessionInfo.entity.notification
      )
    }
    GetCalendarSessionsInMonthResponse(knolxSessions)
  }
}
