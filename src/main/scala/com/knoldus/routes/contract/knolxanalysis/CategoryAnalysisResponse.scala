package com.knoldus.routes.contract.knolxanalysis

import play.api.libs.json.{ Json, OWrites }

final case class CategoryAnalysisResponse(totalSession: Int, categoryInformation: List[CategoryInformation])

object CategoryAnalysisResponse {
  implicit val CategoryAnalysisResponseWrites: OWrites[CategoryAnalysisResponse] = Json.writes[CategoryAnalysisResponse]

  def fromDomain(totalSessions: Int, categoryInformation: List[CategoryInformation]): CategoryAnalysisResponse =
    CategoryAnalysisResponse(totalSessions, categoryInformation)
}
