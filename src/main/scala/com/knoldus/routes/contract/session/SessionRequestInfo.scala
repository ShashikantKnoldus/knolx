package com.knoldus.routes.contract.session

import java.util.Date

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.SessionRequest
import play.api.libs.json.{ Json, OWrites }

final case class SessionRequestInfo(
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

object SessionRequestInfo {
  implicit val SessionRequestInfoWrites: OWrites[SessionRequestInfo] = Json.writes[SessionRequestInfo]

  def fromDomain(slots: Seq[WithId[SessionRequest]]): Seq[SessionRequestInfo] = {
    val freeSlots = slots.map { freeSlot =>
      SessionRequestInfo(
        freeSlot.entity.email,
        freeSlot.entity.date,
        freeSlot.entity.category,
        freeSlot.entity.subCategory,
        freeSlot.entity.topic,
        freeSlot.entity.meetup,
        freeSlot.entity.brief,
        freeSlot.entity.approved,
        freeSlot.entity.decline,
        freeSlot.entity.freeSlot,
        freeSlot.entity.recommendationId,
        freeSlot.entity.notification,
        freeSlot.id
      )
    }
    freeSlots
  }
}
