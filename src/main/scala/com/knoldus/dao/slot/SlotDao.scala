package com.knoldus.dao.slot

import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.mongo.utils.BsonHelpers.DocumentExtensions
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.slot.Slot
import org.mongodb.scala.bson.{ BsonBoolean, BsonInt32, BsonInt64, BsonString }
import org.mongodb.scala.{ Document, MongoDatabase }
import org.mongodb.scala.model.Filters.{ equal, gte, lte }
import scala.util.Try

object SlotDao {
  case object SlotId extends Field

  final case class SlotId(id: String) extends Filter

  final case class SlotType(slotType: String) extends Filter

  final case class DateTime(dateTime: Long) extends Filter

  final case class StartDateTime(dateTime: Long) extends Filter

  final case class EndDateTime(dateTime: Long) extends Filter

  final case class Bookable(bookable: Boolean) extends Filter

  final case class CreatedBy(createdBy: String) extends Filter

  final case class CreatedOn(createdOn: Long) extends Filter

  final case class SessionId(sessionId: Option[String]) extends Filter

  final case class SlotDuration(slotDuration: Int) extends Filter
}

class SlotDao(protected val database: MongoDatabase) extends MongoEntityDao[Slot] {
  import com.knoldus.dao.slot.SlotDao._

  override val collectionName: String = "slots"

  override protected val fieldMapper: Map[Field, String] =
    Map(SlotId -> "id")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case SlotId(id) => Try(equal("id", id))
    case SlotType(slotType) => Try(equal("slotType", slotType))
    case DateTime(dateTime) => Try(gte("dateTime", dateTime))
    case StartDateTime(dateTime) => Try(gte("dateTime", dateTime))
    case EndDateTime(dateTime) => Try(lte("dateTime", dateTime))
    case Bookable(bookable) => Try(equal("bookable", bookable))
    case CreatedBy(createdBy) => Try(equal("createdBy", createdBy))
    case CreatedOn(createdOn) => Try(equal("createdOn", createdOn))
    case SessionId(sessionId) => Try(equal("sessionId", sessionId))
    case SlotDuration(slotDuration) => Try(equal("slotDuration", slotDuration))
  }

  override def entityToDocument(entity: Slot): Document =
    Document(
      "slotType" -> BsonString(entity.slotType),
      "dateTime" -> BsonInt64(entity.dateTime),
      "bookable" -> BsonBoolean(entity.bookable),
      "createdBy" -> BsonString(entity.createdBy),
      "createdOn" -> BsonInt64(entity.createdOn),
      "sessionId" -> entity.sessionId.map(BsonString(_)),
      "slotDuration" -> BsonInt32(entity.slotDuration)
    )

  override def documentToEntity(document: Document): Try[Slot] =
    Try(
      Slot(
        slotType = document.getMandatory[BsonString]("slotType").getValue,
        dateTime = document.getMandatory[BsonInt64]("dateTime").getValue,
        bookable = document.getMandatory[BsonBoolean]("bookable").getValue,
        createdBy = document.getMandatory[BsonString]("createdBy").getValue,
        createdOn = document.getMandatory[BsonInt64]("createdOn").getValue,
        sessionId = document.get[BsonString]("sessionId").map(_.getValue),
        slotDuration = document.getMandatory[BsonInt32]("slotDuration").getValue
      )
    )
}
