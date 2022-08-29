package com.knoldus.domain.slot

import scala.util.Try

object SlotType extends Enumeration {
  type SlotType = Value
  lazy val validValues: Set[SlotType] = values.filterNot(_ == INVALID_SlOT)

  val KNOLX = Value("Knolx")
  val MEETUP = Value("Meetup")
  val WEBINAR = Value("Webinar")
  val KNOLMEET = Value("Knolmeet")
  val INVALID_SlOT = Value("INVALID SLOT TYPE")

  def validateSlotType(slotType: String): SlotType =
    Try(SlotType.withName(slotType)).getOrElse(SlotType.INVALID_SlOT)
}
