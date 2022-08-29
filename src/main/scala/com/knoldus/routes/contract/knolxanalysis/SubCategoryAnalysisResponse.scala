package com.knoldus.routes.contract.knolxanalysis

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Session
import play.api.libs.json.{ Json, OWrites }

final case class SubCategoryAnalysisResponse(subCategoryInfo: List[SubCategoryInformation])

object SubCategoryAnalysisResponse {

  implicit val SubCategoryAnalysisResponseWrites: OWrites[SubCategoryAnalysisResponse] =
    Json.writes[SubCategoryAnalysisResponse]

  def fromDomain(sessions: Seq[WithId[Session]]): SubCategoryAnalysisResponse = {
    val listOfSessions = sessions.map { session =>
      session.entity
    }
    val subCategoryAnalysisResponse = listOfSessions
      .groupBy(_.subCategory)
      .map {
        case (subCategory, session) =>
          SubCategoryInformation(subCategory, session.length)
      }
      .toList
    SubCategoryAnalysisResponse(subCategoryAnalysisResponse)
  }

}
