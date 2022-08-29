package com.knoldus.dao.feedbackform

import com.knoldus.dao.feedbackform.FeedbackFormDao.{ ActiveFeedbackForms, Name }
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.filters.Filters.{ NameIs, SearchQuery }
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.mongo.utils.BsonHelpers._
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.feedbackform.{ FeedbackForm, Question }
import org.mongodb.scala.bson.{ BsonArray, BsonBoolean, BsonDocument, BsonString }
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{ Document, MongoDatabase }

import scala.util.Try

object FeedbackFormDao {
  final case class ActiveFeedbackForms(status: Boolean) extends Filter
  case object Name extends Field
}

// $COVERAGE-OFF$
class FeedbackFormDao(protected val database: MongoDatabase) extends MongoEntityDao[FeedbackForm] {
  override val collectionName: String = "feedbackforms"

  override protected val fieldMapper: Map[Field, String] =
    Map(Name -> "name")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case NameIs(name) => Try(equal("email", name))
    case SearchQuery(email) => Try(equal("email", email))
    case ActiveFeedbackForms(status) => Try(equal("active", status))
  }

  override protected def documentToEntity(document: Document): Try[FeedbackForm] =
    Try(
      FeedbackForm(
        name = document.getMandatory[BsonString]("name").getValue,
        questions = document.getMandatory[BsonArray]("questions").map(ele => toQuestion(ele.asDocument())),
        active = document.getMandatory[BsonBoolean]("active").getValue
      )
    )

  override protected def entityToDocument(entity: FeedbackForm): Document = {
    val document = Document(
      "name" -> BsonString(entity.name),
      "questions" -> entity.questions.map(question => questionToDocument(question)).map(BsonDocument(_)),
      "active" -> BsonBoolean(entity.active)
    )
    document
  }

  def toQuestion(document: Document): Question =
    Question(
      question = document.getMandatory[BsonString]("question").getValue,
      options = document.getMandatory[BsonArray]("options").map(res => res.asString().getValue),
      questionType = document.getMandatory[BsonString]("questionType").getValue,
      mandatory = document.getMandatory[BsonBoolean]("mandatory").getValue
    )

  def questionToDocument(entity: Question): Document = {
    val document = Document(
      "question" -> BsonString(entity.question),
      "options" -> entity.options.map(BsonString(_)),
      "questionType" -> BsonString(entity.questionType),
      "mandatory" -> BsonBoolean(entity.mandatory)
    )
    document
  }
}
// $COVERAGE-ON$
