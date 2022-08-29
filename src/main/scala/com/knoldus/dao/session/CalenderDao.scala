package com.knoldus.dao.session

import java.util.Date

import org.mongodb.scala.model.Filters._
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.session.CalenderDao.{
  Approved,
  Declined,
  Email,
  EndDate,
  FreeSlot,
  Notification,
  StartDate,
  Topic
}
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.session.SessionRequest
import org.mongodb.scala.{ Document, MongoDatabase }
import com.knoldus.dao.mongo.utils.BsonHelpers._
import org.mongodb.scala.bson.{ BsonBoolean, BsonDateTime, BsonString }

import scala.util.Try

// $COVERAGE-OFF$
object CalenderDao {
  case object Email extends Field
  final case class FreeSlot(freeSlot: Boolean) extends Filter
  final case class Notification(notification: Boolean) extends Filter
  final case class Email(email: String) extends Filter
  final case class Topic(topic: String) extends Filter
  final case class Approved(approved: Boolean) extends Filter
  final case class Declined(decline: Boolean) extends Filter
  final case class StartDate(startDate: Date) extends Filter
  final case class EndDate(endDate: Date) extends Filter
}

class CalenderDao(protected val database: MongoDatabase) extends MongoEntityDao[SessionRequest] {

  override val collectionName: String = "sessionrequest"

  override protected val fieldMapper: Map[Field, String] =
    Map(Email -> "email")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case FreeSlot(slot) => Try(equal("freeSlot", slot))
    case Notification(notification) => Try(equal("notification", notification))
    case Email(email) => Try(regex("email", email))
    case Topic(topic) => Try(regex("topic", topic, options = "i"))
    case Approved(approved) => Try(equal("approved", approved))
    case Declined(declined) => Try(equal("decline", declined))
    case StartDate(startDate) => Try(gte("date", startDate))
    case EndDate(endDate) => Try(lte("date", endDate))
  }

  override def entityToDocument(entity: SessionRequest): Document =
    Document(
      "approved" -> BsonBoolean(entity.approved),
      "category" -> BsonString(entity.category),
      "date" -> BsonDateTime(entity.date),
      "decline" -> BsonBoolean(entity.decline),
      "email" -> BsonString(entity.email),
      "freeSlot" -> BsonBoolean(entity.freeSlot),
      "meetup" -> BsonBoolean(entity.meetup),
      "notification" -> BsonBoolean(entity.notification),
      "recommendationId" -> BsonString(entity.recommendationId),
      "subCategory" -> BsonString(entity.subCategory),
      "topic" -> BsonString(entity.topic),
      "brief" -> BsonString(entity.brief)
    )

  override def documentToEntity(document: Document): Try[SessionRequest] =
    Try(
      SessionRequest(
        approved = document.getMandatory[BsonBoolean]("approved").getValue,
        category = document.getMandatory[BsonString]("category").getValue,
        date = new Date(document.getMandatory[BsonDateTime]("date").getValue),
        decline = document.getMandatory[BsonBoolean]("decline").getValue,
        email = document.getMandatory[BsonString]("email").getValue,
        freeSlot = document.getMandatory[BsonBoolean]("freeSlot").getValue,
        meetup = document.getMandatory[BsonBoolean]("meetup").getValue,
        notification = document.getMandatory[BsonBoolean]("notification").getValue,
        recommendationId = document.getMandatory[BsonString]("recommendationId").getValue,
        subCategory = document.getMandatory[BsonString]("subCategory").getValue,
        topic = document.getMandatory[BsonString]("topic").getValue,
        brief = document.getMandatory[BsonString]("brief").getValue
      )
    )
}
// $COVERAGE-ON$
