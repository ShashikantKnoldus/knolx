package com.knoldus.routes.contract.statistic

import com.knoldus.domain.statistic.KnolxDetails
import play.api.libs.json.{ Json, OWrites }

final case class KnolxDetailsResponse(
  id: String,
  title: String,
  dateOfSession: String
)

object KnolxDetailsResponse {

  implicit val KnolxDetailsResponseWrites: OWrites[KnolxDetailsResponse] = Json.writes[KnolxDetailsResponse]

  def fromDomain(knolxDetails: Seq[KnolxDetails]): Seq[KnolxDetailsResponse] =
    knolxDetails.map { knolx =>
      KnolxDetailsResponse(knolx.id, knolx.title, knolx.dateOfSession)
    }
}
