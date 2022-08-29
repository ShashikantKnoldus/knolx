package com.knoldus.domain.info

import scala.concurrent.duration.Duration

final case class Status(uptime: Duration, apiVersion: String)
