package com.knoldus.domain.session

import com.knoldus.routes.contract.session.PresenterDetails
import play.api.libs.json.{ Json, OWrites, Reads }

final case class SessionDetails(
  id: String,
  topic: String,
  presenter: PresenterDetails,
  coPresenter: Option[PresenterDetails]
)

object SessionDetails {
  implicit val sessionDetailsWrites: OWrites[SessionDetails] = Json.writes[SessionDetails]
  implicit val sessionDetailsReads: Reads[SessionDetails] = Json.reads[SessionDetails]
}
