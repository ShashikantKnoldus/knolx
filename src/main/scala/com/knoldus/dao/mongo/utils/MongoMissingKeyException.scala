package com.knoldus.dao.mongo.utils

import org.mongodb.scala.Document

final case class MongoMissingKeyException(
  document: Document,
  key: String
) extends RuntimeException(
      s"No '$key' value found feedbackForm Mongo document. Document: ${document.toJson()}"
    )
