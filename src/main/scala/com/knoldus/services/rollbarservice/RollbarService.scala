package com.knoldus.services.rollbarservice

import com.knoldus.dao.errormanagement.ErrorManagement
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder.withAccessToken
import com.typesafe.config.Config

class RollbarService(config: Config) extends ErrorManagement {

  val rollbarConfig: Config = config.getConfig("rollbar")
  val accessToken: String = rollbarConfig.getString("withAccessToken")
  val environmentRollBar: String = rollbarConfig.getString("environment")
  val codeVersionRollBar: String = config.getString("api-version")
  val frameworkRollBar: String = rollbarConfig.getString("framework")
  val languageRollBar: String = rollbarConfig.getString("language")

  private val configRollbar = withAccessToken(accessToken)
    .environment(environmentRollBar)
    .codeVersion(codeVersionRollBar)
    .framework(frameworkRollBar)
    .language(languageRollBar)
    .build

  override def init(): Unit =
    Rollbar.init(configRollbar)

  override def logError(t: Throwable): Unit = {
    val rollBar = new Rollbar(configRollbar)
    rollBar.log(t)
    rollBar.close(true)
  }
}
