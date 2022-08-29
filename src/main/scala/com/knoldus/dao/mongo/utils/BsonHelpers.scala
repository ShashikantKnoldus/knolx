package com.knoldus.dao.mongo.utils

import org.mongodb.scala.Document
import org.mongodb.scala.bson.{ BsonArray, BsonDocument, BsonValue }
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

// $COVERAGE-OFF$

object BsonHelpers {

  implicit class DocumentExtensions(document: Document) {

    def getChild(key: String): Option[Document] = document.get[BsonDocument](key).map(Document(_))

    def getChildMandatory(key: String): Document = Document(document.getMandatory[BsonDocument](key))

    def getMandatory[B <: BsonValue](key: String)(implicit ct: ClassTag[B]): B =
      if (document.contains(key))
        document.get[B](key) match {
          case Some(value) => value
          case None => throw MongoInvalidValueException(document, key, ct.toString)
        }
      else throw MongoMissingKeyException(document, key)
  }

  implicit class BsonArrayExtensions(array: BsonArray) {

    def map[B](f: BsonValue => B): List[B] =
      asScala.map(f)

    def asScala: List[BsonValue] = array.getValues.asScala.toList

  }

}
// $COVERAGE-ON$
