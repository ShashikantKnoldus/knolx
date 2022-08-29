package com.knoldus.services.email

import akka.Done

import scala.concurrent.{ ExecutionContext, Future }

trait MailerService {

  def sendMessage(receivers: List[String], subject: String, content: String)(implicit
    ec: ExecutionContext
  ): Future[Done]

  def sendMessage(receiver: String, subject: String, content: String)(implicit ec: ExecutionContext): Future[Done]
}
