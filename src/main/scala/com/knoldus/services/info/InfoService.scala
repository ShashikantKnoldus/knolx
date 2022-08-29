package com.knoldus.services.info

import java.lang.management.ManagementFactory
import akka.event.LoggingAdapter
import com.knoldus.bootstrap.DaoInstantiator
import com.knoldus.dao.user.UserDao.CheckActiveUsers
import com.knoldus.domain.info.Status

import scala.concurrent.duration.{ Duration, MILLISECONDS }
import scala.concurrent.{ Await, ExecutionContext, Future }
import com.typesafe.config.Config

import scala.util.{ Failure, Success, Try }

class InfoService(config: Config, daoInstantiator: DaoInstantiator)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {

  def getUptime: Future[Status] =
    Future {
      Status(Duration(ManagementFactory.getRuntimeMXBean.getUptime, MILLISECONDS), config.getString("api-version"))
    }

  def getDbStatus: Boolean = {
    val users = daoInstantiator.userDao.count(CheckActiveUsers(true))
    val dbCallResult = Try(Await.ready(users, Duration("1 seconds")))

    dbCallResult match {
      case Success(_) => true
      case Failure(_) => false
    }
  }
}
