package com.knoldus.utils

import com.knoldus.BaseSpec
import com.knoldus.utils.StringExtensions._

class StringExtensionsSpec extends BaseSpec {

  "StringOps#toOption" should {

    "convert empty string to None" in {
      "".toOption shouldBe None
    }

    "convert nonempty string to Some" in {
      "str".toOption shouldBe Some("str")
    }

  }

}
