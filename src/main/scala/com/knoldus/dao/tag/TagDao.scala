package com.knoldus.dao.tag

import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.mongo.utils.BsonHelpers.DocumentExtensions
import com.knoldus.dao.sorting.Field
import com.knoldus.dao.tag.TagDao.{ Id, Name }
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.Filters.{ equal, regex }
import org.mongodb.scala.{ Document, MongoDatabase }
import com.knoldus.domain.Tag.Tag

import scala.util.Try

object TagDao {

  case object Id extends Field

  final case class Id(id: String) extends Filter

  final case class Name(name: String) extends Filter

}

class TagDao(protected val database: MongoDatabase) extends MongoEntityDao[Tag] {

  override val collectionName: String = "tag"

  override protected val fieldMapper: Map[Field, String] =
    Map(Id -> "id")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case Id(id) => Try(equal("id", id))
    case Name(name) => Try(regex("name", name))

  }

  override protected def documentToEntity(tagDocument: Document): Try[Tag] =
    Try(
      Tag(
        id = tagDocument.getMandatory[BsonString]("id").getValue,
        name = tagDocument.getMandatory[BsonString]("name").getValue
      )
    )

  override protected def entityToDocument(entity: Tag): Document =
    Document(
      "id" -> BsonString(entity.id),
      "name" -> BsonString(entity.name)
    )
}
