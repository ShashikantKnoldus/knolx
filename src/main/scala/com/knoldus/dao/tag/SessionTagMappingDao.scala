package com.knoldus.dao.tag

import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.mongo.utils.BsonHelpers._
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.sessionTagmapping.SessionTagMapping
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{ Document, MongoDatabase }
import scala.util.Try

// $COVERAGE-OFF$
final case class CheckSession(sessionId: String) extends Filter
final case class CheckTag(tagId: String) extends Filter

case object SessionId extends Field
case object TagId extends Field

class SessionTagMappingDao(protected val database: MongoDatabase) extends MongoEntityDao[SessionTagMapping] {

  override val collectionName: String = "session_tag_mapping"

  override protected val fieldMapper: Map[Field, String] =
    Map(SessionId -> "sessionId")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case CheckSession(id) => Try(equal("sessionId", id))
    case CheckTag(id) => Try(equal("tagId", id))
  }

  override protected def documentToEntity(sessionDocument: Document): Try[SessionTagMapping] =
    Try(
      SessionTagMapping(
        sessionId = sessionDocument.getMandatory[BsonString]("sessionId").getValue,
        tagId = sessionDocument.getMandatory[BsonString]("tagId").getValue
      )
    )

  override protected def entityToDocument(entity: SessionTagMapping): Document =
    Document(
      "sessionId" -> BsonString(entity.sessionId),
      "tagId" -> BsonString(entity.tagId)
    )
}
// $COVERAGE-ON$
