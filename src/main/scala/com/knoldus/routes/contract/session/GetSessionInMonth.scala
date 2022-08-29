package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Session
import play.api.libs.json.{ Json, OWrites }

final case class GetSessionInMonth(sessions: Seq[CalendarSession])

object GetSessionInMonth {
  implicit val GetSessionInMonthWrites: OWrites[GetSessionInMonth] = Json.writes[GetSessionInMonth]

  def fromDomain(sessions: Seq[WithId[Session]]): GetSessionInMonth = {
    val knolxSession = sessions.map { sessionInfo =>
      CalendarSession(
        sessionInfo.id,
        sessionInfo.entity.date,
        sessionInfo.entity.email,
        sessionInfo.entity.topic,
        sessionInfo.entity.meetup,
        sessionInfo.entity.brief,
        sessionInfo.entity.date.toString,
        approved = true,
        decline = false,
        pending = false,
        freeSlot = false,
        contentAvailable = sessionInfo.entity.youtubeURL.isDefined || sessionInfo.entity.slideShareURL.isDefined,
        notification = false
      )
    }
    GetSessionInMonth(knolxSession)
  }
}
