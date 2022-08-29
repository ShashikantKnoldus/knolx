package com.knoldus.routes.contract.usermanagement

import play.api.libs.json.{ Json, Reads }

final case class ChangeParticipantStatusResponse(
  email: String,
  nonParticipating: Boolean
)

object ChangeParticipantStatusResponse {

  implicit val changeParticipantStatusRead: Reads[ChangeParticipantStatusResponse] =
    Json.reads[ChangeParticipantStatusResponse]
}
