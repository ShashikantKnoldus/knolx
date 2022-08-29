package com.knoldus.dao.filters

// $COVERAGE-OFF$
final case class And(left: Filter, right: Filter) extends Filter {

  override def equals(that: Any): Boolean =
    that match {
      case And(`left`, `right`) | And(`right`, `left`) => true
      case _ => false
    }

  override def hashCode(): Int = left.hashCode() * right.hashCode()

}
// $COVERAGE-ON$
