package com.knoldus.routes.contract.session

import java.util.Date

import play.api.libs.json.{ Json, Reads }

final case class UpdateApproveSessionInfo(
  date: Date,
  sessionId: String = "",
  topic: String = "Free slot",
  email: String = "",
  category: String = "",
  subCategory: String = "",
  meetup: Boolean = false,
  brief: String = "",
  approved: Boolean = false,
  decline: Boolean = false,
  freeSlot: Boolean = false,
  notification: Boolean = false,
  recommendationId: String = ""
)

object UpdateApproveSessionInfo {
  implicit val UpdateApproveSessionInfoReads: Reads[UpdateApproveSessionInfo] = Json.reads[UpdateApproveSessionInfo]
}
