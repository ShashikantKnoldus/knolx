package com.knoldus.routes.contract.knolxanalysis

import play.api.libs.json.{ Json, OWrites }

final case class KnolxMonthlyInfo(monthName: String, total: Int)

object KnolxMonthlyInfo {
  implicit val KnolxMonthlyInfoWrites: OWrites[KnolxMonthlyInfo] = Json.writes[KnolxMonthlyInfo]
}
