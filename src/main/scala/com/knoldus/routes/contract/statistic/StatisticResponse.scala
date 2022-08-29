package com.knoldus.routes.contract.statistic

import com.knoldus.domain.statistic.Statistic
import play.api.libs.json.{ Json, OWrites }

final case class StatisticResponse(
  userId: String,
  email: String,
  knolxCount: Int,
  knolxDetails: Seq[KnolxDetailsResponse]
)

object StatisticResponse {

  implicit val GetStatisticResponseWrites: OWrites[StatisticResponse] = Json.writes[StatisticResponse]

  def fromDomain(statistics: Seq[Statistic]): Seq[StatisticResponse] =
    statistics.map { statistic =>
      StatisticResponse(
        statistic.userId,
        statistic.email,
        statistic.knolxCount,
        KnolxDetailsResponse.fromDomain(statistic.knolxDetails)
      )
    }
}
