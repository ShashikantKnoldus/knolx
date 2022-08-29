package com.knoldus.dao.filters

object Filters {
  final case class NameIs(name: String) extends Filter
  final case class SearchQuery(term: String) extends Filter
}
