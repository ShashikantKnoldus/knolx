package com.knoldus.utils

import com.knoldus.BaseSpec
import com.knoldus.dao.session.SessionDao.DateMatch
import com.knoldus.dao.sorting.Direction.Descending
import com.knoldus.dao.sorting.SortBy

class SortBySpec extends BaseSpec {

  val sortBy = SortBy

  "Sort by fields" should {
    "do Sort by fields in Descending" in {
      val result = sortBy.apply(DateMatch, Descending)
      assert(result.fields.nonEmpty)
    }
    "give error in Sort" in {
      val result = intercept[Exception] {
        sortBy.apply()
      }
      assert(result.getMessage.nonEmpty)
    }
  }
}
