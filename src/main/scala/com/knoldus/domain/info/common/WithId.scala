package com.knoldus.domain.info.common

final case class WithId[T](id: String, entity: T)
