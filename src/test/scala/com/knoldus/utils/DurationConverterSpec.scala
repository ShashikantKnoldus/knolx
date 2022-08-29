package com.knoldus.utils

import java.time.Duration
import java.util.concurrent.TimeUnit

import com.knoldus.BaseSpec
import com.knoldus.utils.DurationConverter.toScalaFiniteDuration

import scala.concurrent.duration.FiniteDuration

class DurationConverterSpec extends BaseSpec {

  "DurationConverter#toScalaFiniteDuration" should {

    "convert Duration to FiniteDuration properly" in {
      toScalaFiniteDuration(Duration.ofMillis(3)) shouldBe FiniteDuration(3000000, TimeUnit.NANOSECONDS)
    }

  }

}
