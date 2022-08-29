package com.knoldus.domain.common

final case class WithId[T](id: String, entity: T)
