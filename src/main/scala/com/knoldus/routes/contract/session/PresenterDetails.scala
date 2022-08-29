package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites, Reads }

case class PresenterDetails(fullName: String, email: String, userProfilePic: Option[String] = None)

object PresenterDetails {
  implicit val presenterDetailsWrites: OWrites[PresenterDetails] = Json.writes[PresenterDetails]
  implicit val presenterDetailsReads: Reads[PresenterDetails] = Json.reads[PresenterDetails]
}
