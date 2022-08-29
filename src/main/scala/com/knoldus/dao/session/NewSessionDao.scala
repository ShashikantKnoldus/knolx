package com.knoldus.dao.session

import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.session.NewSession
import org.mongodb.scala.bson.{ conversions, BsonArray, BsonInt32, BsonInt64, BsonString }
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{ Document, MongoCollection, MongoDatabase }

import scala.util.Try
import com.knoldus.dao.mongo.utils.BsonHelpers._
import com.knoldus.routes.contract.session.PresenterDetails
import org.mongodb.scala.model.Projections.{ excludeId, fields, include }

import scala.concurrent.Future

// $COVERAGE-OFF$
object NewSessionDao {

  final case class SessionId(sionId: String) extends Filter

  final case class Presenter(presenterDetails: PresenterDetails) extends Filter

  final case class CoPresenter(coPresenterDetails: PresenterDetails) extends Filter

  final case class DateTime(dateTime: Long) extends Filter

  final case class Upcoming(dateTime: Long) extends Filter

  final case class DateTimeStart(dateTime: Long) extends Filter

  final case class DateTimeEnd(dateTime: Long) extends Filter

  final case class SessionDuration(sessionDuration: Int) extends Filter

  final case class Topic(topic: String) extends Filter

  final case class Category(category: String) extends Filter

  final case class SubCategory(subCategory: String) extends Filter

  final case class FeedbackFormId(feedbackFormId: Option[String]) extends Filter

  final case class FeedbackExpirationDate(feedbackExpirationDate: Long) extends Filter

  final case class SessionType(sessionType: String) extends Filter

  final case class MatchPresenter(presenter: PresenterDetails) extends Filter

  final case class MatchCoPresenter(coPresenter: PresenterDetails) extends Filter

  final case class TopicMatch(topic: String) extends Filter

  final case class SessionState(sessionState: String) extends Filter

  final case class SessionsExcept(sessionState: String) extends Filter

  final case class SessionDescription(sessionDescription: String) extends Filter

  case object DateMatch extends Field

  final case class YoutubeURL(youtubeURL: Option[String]) extends Filter

  final case class SlideShareURL(slideShareURL: Option[String]) extends Filter

  final case class SlideURL(slideURL: Option[String]) extends Filter

  final case class SlidesApprovedBy(slidesApprovedBy: Option[String]) extends Filter

  final case class SessionApprovedBy(sessionApprovedBy: Option[String]) extends Filter

}

class NewSessionDao(protected val database: MongoDatabase) extends MongoEntityDao[NewSession] {

  import com.knoldus.dao.session.NewSessionDao._

  override val collectionName: String = "sessions"

  val sessionCollection: MongoCollection[Document] = database.getCollection("sessions")

  override protected val fieldMapper: Map[Field, String] =
    Map(DateMatch -> "dateTime")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case Presenter(presenterDetails) =>
      Try(
        equal("presenterDetails", Document("fullName" -> presenterDetails.fullName, "email" -> presenterDetails.email))
      )
    case CoPresenter(coPresenterDetails) =>
      Try(
        equal(
          "coPresenterDetails",
          Document("fullName" -> coPresenterDetails.fullName, "email" -> coPresenterDetails.email)
        )
      )
    case SessionId(sessionId) => Try(equal("id", sessionId))
    case DateTime(dateTime) => Try(lte("dateTime", dateTime))
    case DateTimeStart(dateTime) => Try(gte("dateTime", dateTime))
    case DateTimeEnd(dateTime) => Try(lte("dateTime", dateTime))
    case Upcoming(dateTime) => Try(gte("dateTime", dateTime))
    case MatchPresenter(presenter) => Try(matchPresenter(presenter))
    case MatchCoPresenter(coPresenter) => Try(matchCoPresenter(coPresenter))
    case TopicMatch(topic) => Try(regex("topic", topic, options = "i"))
    case SessionDuration(sessionDuration) => Try(equal("sessionDuration", sessionDuration))
    case Topic(topic) => Try(equal("topic", topic))
    case Category(category) => Try(equal("category", category))
    case SubCategory(subCategory) => Try(equal("subCategory", subCategory))
    case FeedbackFormId(feedBackFormId) => Try(equal("feedBackFormId", feedBackFormId))
    case FeedbackExpirationDate(feedBackExpirationDate) => Try(equal("feedBackExpirationDate", feedBackExpirationDate))
    case SessionType(sessionType) => Try(equal("sessionType", sessionType))
    case SessionState(sessionState) => Try(equal("sessionState", sessionState))
    case SessionsExcept(sessionState) => Try(notEqual("sessionState", sessionState))
    case SessionDescription(sessionDescription) => Try(equal("sessionDescription", sessionDescription))
    case YoutubeURL(youtubeURL) => Try(gt("youtubeURL", youtubeURL))
    case SlideShareURL(slideShareURL) => Try(lt("slideShareURL", slideShareURL))
    case SlideURL(slideURL) => Try(lt("slideURL", slideURL))
    case SlidesApprovedBy(slidesApprovedBy) => Try(lt("slidesApprovedBy", slidesApprovedBy))
    case SessionApprovedBy(sessionApprovedBy) => Try(lt("sessionApprovedBy", sessionApprovedBy))

  }

  def documentToPresenterDetails(document: Document): PresenterDetails =
    if (document.isEmpty)
      PresenterDetails("", "")
    else
      PresenterDetails(
        fullName = document.getMandatory[BsonString]("fullName").getValue,
        email = document.getMandatory[BsonString]("email").getValue
      )

  override protected def documentToEntity(sessionDocument: Document): Try[NewSession] =
    Try(
      NewSession(
        presenterDetails = documentToPresenterDetails(sessionDocument.getChildMandatory("presenterDetails")),
        coPresenterDetails =
          Some(documentToPresenterDetails(sessionDocument.getChild("coPresenterDetails").getOrElse(Document.empty))),
        dateTime = sessionDocument.getMandatory[BsonInt64]("dateTime").getValue,
        sessionDuration = sessionDocument.getMandatory[BsonInt32]("sessionDuration").getValue,
        topic = sessionDocument.getMandatory[BsonString]("topic").getValue,
        category = sessionDocument.getMandatory[BsonString]("category").getValue,
        subCategory = sessionDocument.getMandatory[BsonString]("subCategory").getValue,
        feedbackFormId = sessionDocument.get[BsonString]("feedbackFormId").map(_.getValue),
        feedbackExpirationDate = sessionDocument.getMandatory[BsonInt64]("feedbackExpirationDate").getValue,
        sessionType = sessionDocument.getMandatory[BsonString]("sessionType").getValue,
        sessionState = sessionDocument.getMandatory[BsonString]("sessionState").getValue,
        sessionDescription = sessionDocument.getMandatory[BsonString]("sessionDescription").getValue,
        youtubeURL = sessionDocument.get[BsonString]("youtubeURL").map(_.getValue),
        slideShareURL = sessionDocument.get[BsonString]("slideShareURL").map(_.getValue),
        slideURL = sessionDocument.get[BsonString]("slideURL").map(_.getValue),
        slidesApprovedBy = sessionDocument.get[BsonString]("slidesApprovedBy").map(_.getValue),
        sessionApprovedBy = sessionDocument.get[BsonString]("sessionApprovedBy").map(_.getValue),
        sessionTag = sessionDocument.getMandatory[BsonArray]("sessionTag").map(_.asString().getValue),
        remarks = sessionDocument.get[BsonString]("remarks").map(_.getValue)
      )
    )

  def presenterDetailsToDocument(entity: PresenterDetails): Document =
    Document(
      "fullName" -> BsonString(entity.fullName),
      "email" -> BsonString(entity.email)
    )

  def matchPresenter(entity: PresenterDetails): conversions.Bson =
    regex("presenterDetails.email", entity.email)

  def matchCoPresenter(entity: PresenterDetails): conversions.Bson =
    regex("coPresenterDetails.email", entity.email)

  def fetchTags(tag: String): Future[Option[Seq[String]]] =
    sessionCollection
      .find(regex("sessionTag", tag))
      .projection(fields(include("sessionTag"), excludeId()))
      .map(doc =>
        doc.getMandatory[BsonArray]("sessionTag").map(elem => elem.asString().getValue).filter { sessionTag =>
          sessionTag.matches(tag + ".*")
        }
      )
      .collect()
      .map { tags =>
        tags.flatten.distinct
      }
      .headOption()

  override protected def entityToDocument(entity: NewSession): Document =
    Document(
      "presenterDetails" -> presenterDetailsToDocument(entity.presenterDetails),
      "coPresenterDetails" -> entity.coPresenterDetails.fold(Document.empty)(_ =>
            presenterDetailsToDocument(entity.coPresenterDetails.get)
          ),
      "dateTime" -> BsonInt64(entity.dateTime),
      "sessionDuration" -> BsonInt32(entity.sessionDuration),
      "topic" -> BsonString(entity.topic),
      "category" -> BsonString(entity.category),
      "subCategory" -> BsonString(entity.subCategory),
      "feedbackFormId" -> entity.feedbackFormId.map(BsonString(_)),
      "feedbackExpirationDate" -> BsonInt64(entity.feedbackExpirationDate),
      "sessionType" -> BsonString(entity.sessionType),
      "sessionState" -> BsonString(entity.sessionState),
      "sessionDescription" -> BsonString(entity.sessionDescription),
      "youtubeURL" -> entity.youtubeURL.map(BsonString(_)),
      "slideShareURL" -> entity.slideShareURL.map(BsonString(_)),
      "slideURL" -> entity.slideURL.map(BsonString(_)),
      "slidesApprovedBy" -> entity.slidesApprovedBy.map(BsonString(_)),
      "sessionApprovedBy" -> entity.sessionApprovedBy.map(BsonString(_)),
      "sessionTag" -> entity.sessionTag.map(BsonString(_)),
      "remarks" -> entity.remarks.map(BsonString(_))
    )
}
// $COVERAGE-ON$
