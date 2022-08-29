package com.knoldus.services.scheduler

object SessionState extends Enumeration {

  type SessionState = Value

  val ExpiringNext, ExpiringNextNotReminded, SchedulingNext, SchedulingNextUnNotified = Value

}
