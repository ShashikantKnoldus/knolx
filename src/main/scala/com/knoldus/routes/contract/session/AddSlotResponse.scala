package com.knoldus.routes.contract.session

import java.util.Date

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.SessionRequest
import play.api.libs.json.{ Json, OWrites }

final case class AddSlotResponse(id: String, slotName: String, date: Date, isNotification: Boolean)

object AddSlotResponse {
  implicit val AddSlotResponseWrites: OWrites[AddSlotResponse] = Json.writes[AddSlotResponse]

  def fromDomain(sessionRequest: WithId[SessionRequest]): AddSlotResponse =
    sessionRequest match {
      case WithId(entity, id) => AddSlotResponse(id, entity.topic, entity.date, entity.notification)
    }
}
