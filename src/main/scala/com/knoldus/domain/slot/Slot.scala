package com.knoldus.domain.slot

final case class Slot(
  slotType: String,
  dateTime: Long,
  bookable: Boolean,
  createdBy: String,
  createdOn: Long,
  sessionId: Option[String],
  slotDuration: Int
)
