package com.knoldus.dao.sorting

import com.knoldus.dao.sorting.Direction.Ascending

/**
  * <<<<<<< HEAD
  * Represents requirement to sort dao entity by field feedbackForm the specific order direction
  *
  * @param fields ''field/direction'' pairs to sort by field with direction multiple times
  */
// TODO rewrite using NonEmptyList and get rid of require check
final case class SortBy(fields: (Field, Direction)*) {
  require(fields.nonEmpty, "Can't sort with no fields specified")
}

object SortBy {

  /**
    * Sort by a single field only
    * @param field Field to sort by
    * @param direction Either ASC or DESC
    */
  def apply(field: Field, direction: Direction = Ascending): SortBy = this(field -> direction)

}
