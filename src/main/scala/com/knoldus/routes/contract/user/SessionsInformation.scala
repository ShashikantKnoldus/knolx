package com.knoldus.routes.contract.user

import play.api.libs.json.{ Json, OWrites }

final case class SessionsInformation(topic: String, coreMemberRating: Double, nonCoreMemberRating: Double)

object SessionsInformation {
  implicit val SessionInfoWrites: OWrites[SessionsInformation] = Json.writes[SessionsInformation]

}
