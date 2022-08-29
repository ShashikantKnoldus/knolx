package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Session
import play.api.libs.json.{ Json, OWrites }

// $COVERAGE-OFF$
final case class GetSessionsInTimeRangeResponse(sessions: Seq[GetSessionByIdResponse])

object GetSessionsInTimeRangeResponse {

  implicit val getSessionsInTimeRangeResponseWrites: OWrites[GetSessionsInTimeRangeResponse] =
    Json.writes[GetSessionsInTimeRangeResponse]

  def fromDomain(sessions: Seq[WithId[Session]]): GetSessionsInTimeRangeResponse = {
    val sessionInfo = sessions.map { session =>
      val fullName = session.entity.email
        .split("@")
        .headOption
        .fold("Invalid") { name =>
          name.split('.').map(_.capitalize).mkString(" ")
        }
        .trim
      GetSessionByIdResponse(
        session.entity.userId,
        session.entity.email,
        fullName,
        session.entity.date,
        session.entity.session,
        session.entity.category,
        session.entity.subCategory,
        session.entity.feedbackFormId,
        session.entity.topic,
        session.entity.brief,
        session.entity.feedbackExpirationDays,
        session.entity.meetup,
        session.entity.rating,
        session.entity.score,
        session.entity.cancelled,
        session.entity.active,
        session.entity.expirationDate,
        session.entity.youtubeURL.fold("")(identity),
        session.entity.slideShareURL.fold("")(identity),
        session.entity.temporaryYoutubeURL,
        session.entity.reminder,
        session.entity.notification,
        session.id
      )
    }
    GetSessionsInTimeRangeResponse(sessionInfo)
  }
}
// $COVERAGE-ON$
