package com.knoldus.domain.department

final case class Department(name: String, headEmail: Option[String], quota: Int, createdOn: Long, lastUpdated: Long)
