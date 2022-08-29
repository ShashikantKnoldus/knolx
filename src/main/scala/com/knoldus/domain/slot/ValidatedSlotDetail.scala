package com.knoldus.domain.slot

import com.knoldus.domain.slot.SlotType.SlotType

final case class ValidatedSlotDetail(slotType: SlotType, dateTime: Long, conflictWithKnolmeetSlot: Boolean)
