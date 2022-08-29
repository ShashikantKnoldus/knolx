package com.knoldus.routes.contract.slot

import com.knoldus.domain.session.GetSlotsResponse
import play.api.libs.json.{ Json, OWrites }

final case class GetFourMonthSlots(slots: Seq[GetSlotsResponse])

object GetFourMonthSlots {
  implicit val getFourMonthSlotsResponseWrites: OWrites[GetFourMonthSlots] = Json.writes[GetFourMonthSlots]

  def fromDomain(slots: Seq[GetSlotsResponse]): GetFourMonthSlots =
    GetFourMonthSlots(slots)
}
