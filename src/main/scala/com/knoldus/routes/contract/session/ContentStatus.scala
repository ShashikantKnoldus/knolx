package com.knoldus.routes.contract.session

sealed trait ContentStatus

object ContentStatus {

  case object Available extends ContentStatus
  case object NotAvailable extends ContentStatus
  case object Cancelled extends ContentStatus

}
