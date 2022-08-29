package com.knoldus.domain.statistic

final case class Statistic(
  userId: String,
  email: String,
  knolxCount: Int,
  knolxDetails: Seq[KnolxDetails]
)
