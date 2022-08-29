package com.knoldus.dao.feedbackform

import java.util.Date

import com.knoldus.dao.feedbackform.FeedbackFormDao.Name
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao._
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.filters.Filters.{ NameIs, SearchQuery }
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.mongo.utils.BsonHelpers._
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.feedbackform.UserFeedbackResponse
import com.knoldus.routes.contract.feedbackform.QuestionResponse
import org.mongodb.scala.bson.{ BsonArray, BsonBoolean, BsonDateTime, BsonDocument, BsonDouble, BsonString }
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{ Document, MongoDatabase }

import scala.util.Try

// $COVERAGE-OFF$
object FeedbackFormsResponseDao {
  final case class StoredUserId(userId: String) extends Filter
  final case class StoredSessionId(sessionId: String) extends Filter
  final case class Meetup(status: Boolean) extends Filter
  final case class FeedbackResponseScore(score: Double) extends Filter
  final case class CheckCoreMember(status: Boolean) extends Filter
  final case class CheckFeedbackMail(email: String) extends Filter
  final case class CheckFeedbackScore(score: Double) extends Filter
  final case class CheckResponse(response: String) extends Filter
  case object Name extends Field
}

class FeedbackFormsResponseDao(protected val database: MongoDatabase) extends MongoEntityDao[UserFeedbackResponse] {
  override val collectionName: String = "feedbackformsresponse"

  override protected val fieldMapper: Map[Field, String] =
    Map(Name -> "name")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case NameIs(name) => Try(equal("email", name))
    case SearchQuery(term) => Try(equal("email", term))
    case StoredUserId(value) => Try(equal("userId", value))
    case Meetup(status) => Try(equal("meetup", status))
    case StoredSessionId(id) => Try(equal("sessionId", id))
    case FeedbackResponseScore(score) => Try(gte("score", score))
    case CheckCoreMember(status) => Try(equal("coreMember", status))
    case CheckFeedbackMail(email) => Try(equal("email", email))
    case CheckFeedbackScore(score) => Try(equal("score", score))
    case CheckResponse(response) => Try(equal("response", response))

  }

  override protected def documentToEntity(document: Document): Try[UserFeedbackResponse] =
    Try(
      UserFeedbackResponse(
        sessionId = document.getMandatory[BsonString]("sessionId").getValue,
        userId = document.getMandatory[BsonString]("userId").getValue,
        coreMember = document.getMandatory[BsonBoolean]("coreMember").getValue,
        email = document.getMandatory[BsonString]("email").getValue,
        feedbackResponse =
          document.getMandatory[BsonArray]("feedbackResponse").map(res => toQuestionResponse(res.asDocument())),
        meetup = document.getMandatory[BsonBoolean]("meetup").getValue,
        presenter = document.getMandatory[BsonString]("presenter").getValue,
        responseDate = new Date(document.getMandatory[BsonDateTime]("responseDate").getValue),
        score = document.getMandatory[BsonDouble]("score").getValue,
        session = document.getMandatory[BsonString]("session").getValue,
        sessionTopic = document.getMandatory[BsonString]("sessionTopic").getValue,
        sessionDate = new Date(document.getMandatory[BsonDateTime]("sessiondate").getValue)
      )
    )

  override protected def entityToDocument(entity: UserFeedbackResponse): Document = {
    val document = Document(
      "sessionId" -> BsonString(entity.sessionId),
      "userId" -> BsonString(entity.userId),
      "coreMember" -> BsonBoolean(entity.coreMember),
      "email" -> BsonString(entity.email),
      "feedbackResponse" -> entity.feedbackResponse
            .map(response => questionResponseToDocument(response))
            .map(BsonDocument(_)),
      "meetup" -> BsonBoolean(entity.meetup),
      "presenter" -> BsonString(entity.presenter),
      "responseDate" -> BsonDateTime(entity.responseDate),
      "score" -> BsonDouble(entity.score),
      "session" -> BsonString(entity.session),
      "sessionTopic" -> BsonString(entity.sessionTopic),
      "sessiondate" -> BsonDateTime(entity.sessionDate)
    )
    document
  }

  def toQuestionResponse(document: Document): QuestionResponse =
    QuestionResponse(
      question = document.getMandatory[BsonString]("question").getValue,
      options = document.getMandatory[BsonArray]("options").map(res => res.asString().getValue),
      response = document.getMandatory[BsonString]("response").getValue
    )

  def questionResponseToDocument(entity: QuestionResponse): Document = {
    val document = Document(
      "question" -> BsonString(entity.question),
      "options" -> entity.options.map(BsonString(_)),
      "response" -> BsonString(entity.response)
    )
    document
  }
}

// $COVERAGE-ON$
