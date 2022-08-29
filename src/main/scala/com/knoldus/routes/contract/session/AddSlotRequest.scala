package com.knoldus.routes.contract.session

import java.util.Date

import play.api.libs.json.{ Json, Reads }

final case class AddSlotRequest(slotName: String, date: Date, isNotification: Boolean)

object AddSlotRequest {
  implicit val AddSlotRequestReads: Reads[AddSlotRequest] = Json.reads[AddSlotRequest]
}
