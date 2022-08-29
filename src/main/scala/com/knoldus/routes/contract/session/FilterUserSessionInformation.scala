package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites }

final case class FilterUserSessionInformation(email: Option[String], startDate: Long, endDate: Long)

object FiterUserSessionInformation {

  implicit val FilterUserSessionInformation: OWrites[FilterUserSessionInformation] =
    Json.writes[FilterUserSessionInformation]
}
