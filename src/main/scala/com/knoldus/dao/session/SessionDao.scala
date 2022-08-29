package com.knoldus.dao.session

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.session.Session

import java.util.Date
import org.mongodb.scala.bson.{ BsonBoolean, BsonDateTime, BsonDouble, BsonInt32, BsonString }
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{ Document, MongoDatabase }

import scala.util.Try
import com.knoldus.dao.mongo.utils.BsonHelpers._

// $COVERAGE-OFF$
object SessionDao {

  final case class Active(term: Boolean) extends Filter

  final case class Pending(date: Date) extends Filter

  final case class Completed(date: Date) extends Filter

  final case class Email(email: String) extends Filter

  final case class Topic(topic: String) extends Filter

  final case class SessionScore(score: Double) extends Filter

  final case class SessionId(id: String) extends Filter

  final case class PresenterSession(id: String) extends Filter

  final case class UserKnolxSession(id: String) extends Filter

  final case class SubCategory(subCategory: String) extends Filter

  final case class CategoryCheck(name: String) extends Filter

  final case class ExpirationDate(date: Date) extends Filter

  final case class SessionExpiryDate(date: Date) extends Filter

  final case class SessionDate(date: Date) extends Filter

  final case class CancelledSession(status: Boolean) extends Filter

  final case class StartDate(startDate: Date) extends Filter

  final case class EndDate(endDate: Date) extends Filter

  final case class Cancelled(cancelled: Boolean) extends Filter

  case object Name extends Field

  case object DateMatch extends Field

  final case class TodayExpiringSession(fromDate: Date, toDate: Date) extends Filter

  final case class CheckReminder(status: Boolean) extends Filter

  final case class CheckNotification(status: Boolean) extends Filter

  final case class Brief(brief: String) extends Filter

}

class SessionDao(protected val database: MongoDatabase) extends MongoEntityDao[Session] {

  import com.knoldus.dao.session.SessionDao._

  override val collectionName: String = "sessions"

  override protected val fieldMapper: Map[Field, String] =
    Map(DateMatch -> "date")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case Active(status) => Try(equal("active", status))
    case Pending(date) => Try(gt("date", date))
    case Completed(date) => Try(lte("date", date))
    case Email(email) => Try(regex("email", email))
    case Topic(topic) => Try(regex("topic", topic, options = "i"))
    case SessionScore(value) => Try(gte("score", value))
    case SessionId(id) => Try(equal("_id", id))
    case ExpirationDate(date) => Try(gt("expirationDate", date))
    case SessionExpiryDate(date) => Try(lt("expirationDate", date))
    case SessionDate(date) => Try(lte("date", date))
    case CancelledSession(status) => Try(equal("cancelled", status))
    case PresenterSession(id) => Try(notEqual("userId", id))
    case UserKnolxSession(id) => Try(equal("userId", id))
    case SubCategory(subCategory) => Try(equal("subCategory", subCategory))
    case CategoryCheck(category) => Try(equal("category", category))
    case StartDate(startDate) => Try(gte("date", startDate))
    case EndDate(endDate) => Try(lte("date", endDate))
    case Cancelled(cancelled) => Try(equal("cancelled", cancelled))
    case TodayExpiringSession(fromDate, toDate) =>
      Try(and(lte("expirationDate", toDate), gte("expirationDate", fromDate)))
    case CheckReminder(status) => Try(equal("reminder", status))
    case CheckNotification(status) => Try(equal("notification", status))
    case Brief(brief) => Try(equal("brief", brief))

  }

  override protected def documentToEntity(sessionDocument: Document): Try[Session] =
    Try(
      Session(
        userId = sessionDocument.getMandatory[BsonString]("userId").getValue,
        email = sessionDocument.getMandatory[BsonString]("email").getValue,
        date = new Date(sessionDocument.getMandatory[BsonDateTime]("date").getValue),
        session = sessionDocument.getMandatory[BsonString]("session").getValue,
        category = sessionDocument.getMandatory[BsonString]("category").getValue,
        subCategory = sessionDocument.getMandatory[BsonString]("subCategory").getValue,
        feedbackFormId = sessionDocument.getMandatory[BsonString]("feedbackFormId").getValue,
        topic = sessionDocument.getMandatory[BsonString]("topic").getValue,
        feedbackExpirationDays = sessionDocument.getMandatory[BsonInt32]("feedbackExpirationDays").getValue,
        meetup = sessionDocument.getMandatory[BsonBoolean]("meetup").getValue,
        brief = sessionDocument.getMandatory[BsonString]("brief").getValue,
        rating = sessionDocument.getMandatory[BsonString]("rating").getValue,
        score = sessionDocument.getMandatory[BsonDouble]("score").getValue,
        cancelled = sessionDocument.getMandatory[BsonBoolean]("cancelled").getValue,
        active = sessionDocument.getMandatory[BsonBoolean]("active").getValue,
        expirationDate = new Date(sessionDocument.getMandatory[BsonDateTime]("expirationDate").getValue),
        youtubeURL = sessionDocument.get[BsonString]("youtubeURL").map(_.getValue),
        slideShareURL = sessionDocument.get[BsonString]("slideShareURL").map(_.getValue),
        temporaryYoutubeURL = sessionDocument.get[BsonString]("temporaryVideoURL").map(_.getValue),
        reminder = sessionDocument.getMandatory[BsonBoolean]("reminder").getValue,
        notification = sessionDocument.getMandatory[BsonBoolean]("notification").getValue
      )
    )

  override protected def entityToDocument(entity: Session): Document =
    Document(
      "userId" -> BsonString(entity.userId),
      "email" -> BsonString(entity.email),
      "date" -> BsonDateTime(entity.date),
      "session" -> BsonString(entity.session),
      "category" -> BsonString(entity.category),
      "subCategory" -> BsonString(entity.subCategory),
      "feedbackFormId" -> BsonString(entity.feedbackFormId),
      "topic" -> BsonString(entity.topic),
      "feedbackExpirationDays" -> BsonInt32(entity.feedbackExpirationDays),
      "meetup" -> BsonBoolean(entity.meetup),
      "brief" -> BsonString(entity.brief),
      "rating" -> BsonString(entity.rating),
      "score" -> BsonDouble(entity.score),
      "cancelled" -> BsonBoolean(entity.cancelled),
      "active" -> BsonBoolean(entity.active),
      "expirationDate" -> BsonDateTime(entity.expirationDate),
      "youtubeURL" -> entity.youtubeURL.map(BsonString(_)),
      "slideShareURL" -> entity.slideShareURL.map(BsonString(_)),
      "temporaryVideoURL" -> entity.temporaryYoutubeURL.map(BsonString(_)),
      "reminder" -> BsonBoolean(entity.reminder),
      "notification" -> BsonBoolean(entity.notification)
    )

  def getOldSessionForMigration(document: Document): Try[WithId[Session]] =
    for {
      entity <- documentToEntity(document)
      id <- Try(document.getObjectId("_id").toString)
    } yield WithId(entity, id)

}
// $COVERAGE-ON$
