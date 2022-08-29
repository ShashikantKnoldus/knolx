package com.knoldus.routes.contract.session

sealed trait SessionStatus

object SessionStatus {

  case object Completed extends SessionStatus
  case object Pending extends SessionStatus
  case object Cancelled extends SessionStatus

}
