package com.knoldus.routes.contract.session

import java.util.Date

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.SessionRequest
import play.api.libs.json.{ Json, OWrites }

final case class GetSessionRequestByUserResponse(
  email: String,
  date: Date,
  category: String,
  subCategory: String,
  topic: String,
  meetup: Boolean,
  brief: String,
  approved: Boolean,
  decline: Boolean,
  freeSlot: Boolean,
  recommendationId: String,
  notification: Boolean,
  id: String
)

object GetSessionRequestByUserResponse {

  implicit val GetSessionRequestByUserResponseWrites: OWrites[GetSessionRequestByUserResponse] =
    Json.writes[GetSessionRequestByUserResponse]

  def fromDomain(sessionRequest: WithId[SessionRequest]): GetSessionRequestByUserResponse =
    sessionRequest match {
      case WithId(entity, id) =>
        GetSessionRequestByUserResponse(
          email = entity.email,
          date = entity.date,
          category = entity.category,
          subCategory = entity.subCategory,
          topic = entity.topic,
          meetup = entity.meetup,
          brief = entity.brief,
          approved = entity.approved,
          decline = entity.decline,
          freeSlot = entity.freeSlot,
          recommendationId = entity.recommendationId,
          notification = entity.notification,
          id
        )
    }
}
