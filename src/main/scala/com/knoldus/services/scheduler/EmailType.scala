package com.knoldus.services.scheduler

trait EmailType

case object Notification extends EmailType
case object Reminder extends EmailType
case object Feedback extends EmailType
