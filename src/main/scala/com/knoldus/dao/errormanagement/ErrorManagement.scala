package com.knoldus.dao.errormanagement

trait ErrorManagement {
  def init(): Unit
  def logError(t: Throwable): Unit
}
