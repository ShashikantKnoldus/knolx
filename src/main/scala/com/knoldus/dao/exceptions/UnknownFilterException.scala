package com.knoldus.dao.exceptions

import com.knoldus.dao.EntityDao
import com.knoldus.dao.filters.Filter

final case class UnknownFilterException(
  filter: Filter,
  dao: EntityDao[_]
) extends RuntimeException(
      s"'$filter' filter is not known to ${dao.getClass.toString}"
    )
