package com.knoldus.dao.exceptions

import com.knoldus.dao.EntityDao
import com.knoldus.dao.sorting.Field

final case class UnknownSortingFieldException(
  field: Field,
  dao: EntityDao[_]
) extends RuntimeException(
      s"'$field' sorting field is not known to ${dao.getClass.toString}"
    )
