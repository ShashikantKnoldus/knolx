package com.knoldus

import com.knoldus.dao.errormanagement.ErrorManagement

class TestErrorManagement extends ErrorManagement {

  override def init(): Unit =
    println("Initialize")

  override def logError(t: Throwable): Unit =
    println(s"Received an error ${t.getMessage}")
}
