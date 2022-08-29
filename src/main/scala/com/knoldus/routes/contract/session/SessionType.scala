package com.knoldus.routes.contract.session

sealed trait SessionType

object SessionType {
  case object Knolx extends SessionType
  case object Meetup extends SessionType
}
