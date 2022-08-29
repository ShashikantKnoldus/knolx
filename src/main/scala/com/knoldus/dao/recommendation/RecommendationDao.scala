package com.knoldus.dao.recommendation

import java.util.Date

import com.knoldus.dao.filters.Filter
import org.mongodb.scala.model.Filters._
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.recommendation.RecommendationDao.{ Approved, Book, DateMatch, Declined, Done, Pending }
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.recommendation.Recommendation
import org.mongodb.scala.{ Document, MongoDatabase }
import com.knoldus.dao.mongo.utils.BsonHelpers._
import org.mongodb.scala.bson._

import scala.util.Try

// $COVERAGE-OFF$
object RecommendationDao {
  final case class Pending(recommendation: Boolean) extends Filter
  final case class Active(term: Boolean) extends Filter
  final case class Approved(recommendation: Boolean) extends Filter
  final case class Done(recommendation: Boolean) extends Filter
  final case class Declined(recommendation: Boolean) extends Filter
  final case class Book(recommendation: Boolean) extends Filter

  final case object Name extends Field
  final case object DateMatch extends Field

}

class RecommendationDao(protected val database: MongoDatabase) extends MongoEntityDao[Recommendation] {

  override val collectionName: String = "recommendations"

  override protected val fieldMapper: Map[Field, String] =
    Map(DateMatch -> "submissionDate")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case Pending(status) => Try(equal("pending", status))
    case Approved(status) => Try(equal("approved", status))
    case Declined(status) => Try(equal("declined", status))
    case Book(status) => Try(equal("book", status))
    case Done(status) => Try(equal("done", status))
  }

  override protected def documentToEntity(recommendationDocument: Document): Try[Recommendation] =
    Try(
      Recommendation(
        email = recommendationDocument.get[BsonString]("email").map(_.getValue),
        name = recommendationDocument.getMandatory[BsonString]("name").getValue,
        topic = recommendationDocument.getMandatory[BsonString]("topic").getValue,
        description = recommendationDocument.getMandatory[BsonString]("description").getValue,
        submissionDate = new Date(recommendationDocument.getMandatory[BsonDateTime]("submissionDate").getValue),
        updateDate = new Date(recommendationDocument.getMandatory[BsonDateTime]("updateDate").getValue),
        approved = recommendationDocument.getMandatory[BsonBoolean]("approved").getValue,
        decline = recommendationDocument.getMandatory[BsonBoolean]("declined").getValue,
        pending = recommendationDocument.getMandatory[BsonBoolean]("pending").getValue,
        done = recommendationDocument.getMandatory[BsonBoolean]("done").getValue,
        book = recommendationDocument.getMandatory[BsonBoolean]("book").getValue,
        upVote = recommendationDocument.getMandatory[BsonInt32]("upVotes").getValue,
        downVote = recommendationDocument.getMandatory[BsonInt32]("downVotes").getValue
      )
    )

  override protected def entityToDocument(entity: Recommendation): Document =
    Document(
      "email" -> entity.email.map(BsonString(_)),
      "name" -> BsonString(entity.name),
      "topic" -> BsonString(entity.topic),
      "description" -> BsonString(entity.description),
      "submissionDate" -> BsonDateTime(entity.submissionDate),
      "updateDate" -> BsonDateTime(entity.updateDate),
      "approved" -> BsonBoolean(entity.approved),
      "declined" -> BsonBoolean(entity.decline),
      "pending" -> BsonBoolean(entity.pending),
      "done" -> BsonBoolean(entity.done),
      "book" -> BsonBoolean(entity.book),
      "upVotes" -> BsonInt32(entity.upVote),
      "downVotes" -> BsonInt32(entity.downVote)
    )

}

// $COVERAGE-ON$
