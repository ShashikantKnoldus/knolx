package com.knoldus.dao.filters

@SuppressWarnings(Array("MethodNames"))
case object TrueFilter extends Filter {
  override def and(that: Filter): Filter = that

  override def or(that: Filter): Filter = this

  override def unary_! : Filter = FalseFilter // scalastyle:off disallow.space.before.token
}
