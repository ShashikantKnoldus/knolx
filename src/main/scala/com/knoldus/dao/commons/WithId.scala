package com.knoldus.dao.commons

final case class WithId[+T](entity: T, id: String)
