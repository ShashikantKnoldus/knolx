package com.knoldus.dao.filters

final case class IdIn(ids: Seq[String]) extends Filter
