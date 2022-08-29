package com.knoldus.dao.clientsecrets

import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.clients.Client
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.{ Document, MongoDatabase }
import org.mongodb.scala.model.Filters.equal
import com.knoldus.dao.mongo.utils.BsonHelpers._

import scala.util.Try

final case class CheckClient(client: String) extends Filter

case object ClientId extends Field

class ClientSecretsDao(protected val database: MongoDatabase) extends MongoEntityDao[Client] {

  override val collectionName: String = "clients"

  override protected val fieldMapper: Map[Field, String] =
    Map(ClientId -> "clientId")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case CheckClient(id) => Try(equal("clientId", id))
  }

  override protected def documentToEntity(sessionDocument: Document): Try[Client] =
    Try(
      Client(
        clientId = sessionDocument.getMandatory[BsonString]("clientId").getValue,
        clientSecret = sessionDocument.getMandatory[BsonString]("clientSecret").getValue
      )
    )

  override protected def entityToDocument(entity: Client): Document =
    Document(
      "clientId" -> BsonString(entity.clientId),
      "clientSecret" -> BsonString(entity.clientSecret)
    )

}
