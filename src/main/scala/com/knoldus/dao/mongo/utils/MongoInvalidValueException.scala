package com.knoldus.dao.mongo.utils

import org.mongodb.scala.Document

final case class MongoInvalidValueException(
  document: Document,
  key: String,
  expectedClass: String
) extends RuntimeException(
      s"'$key' value feedbackForm Mongo document is not of expected type ($expectedClass). Document: ${document.toJson()}"
    )
