package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites }

final case class PrimaryCategory(categories: Seq[String])

object PrimaryCategory {
  implicit val PrimaryCategoryWrites: OWrites[PrimaryCategory] = Json.writes[PrimaryCategory]
}
