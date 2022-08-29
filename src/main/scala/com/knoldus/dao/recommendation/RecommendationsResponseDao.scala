package com.knoldus.dao.recommendation

import com.knoldus.dao.filters.Filter
import org.mongodb.scala.model.Filters._
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.recommendation.RecommendationDao.DateMatch
import com.knoldus.dao.recommendation.RecommendationsResponseDao.{ Email, RecommendationId }
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.recommendation.RecommendationsResponse
import org.mongodb.scala.{ Document, MongoDatabase }
import com.knoldus.dao.mongo.utils.BsonHelpers._
import org.mongodb.scala.bson.{ BsonBoolean, BsonString }

import scala.util.Try

// $COVERAGE-OFF$

object RecommendationsResponseDao {

  final case class RecommendationId(recommendationId: String) extends Filter
  final case class Email(email: String) extends Filter
  case object Name extends Field
  case object DateMatch extends Field

}

class RecommendationsResponseDao(protected val database: MongoDatabase)
    extends MongoEntityDao[RecommendationsResponse] {

  override val collectionName: String = "recommendationsresponse"

  override protected val fieldMapper: Map[Field, String] =
    Map(DateMatch -> "date")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case Email(email) => Try(regex("email", email))
    case RecommendationId(recommendationId) => Try(equal("recommendationId", recommendationId))
  }

  override protected def documentToEntity(recommendationResponseDocument: Document): Try[RecommendationsResponse] =
    Try(
      RecommendationsResponse(
        email = recommendationResponseDocument.getMandatory[BsonString]("email").getValue,
        recommendationId = recommendationResponseDocument.getMandatory[BsonString]("recommendationId").getValue,
        upVote = recommendationResponseDocument.getMandatory[BsonBoolean]("upVote").getValue,
        downVote = recommendationResponseDocument.getMandatory[BsonBoolean]("downVote").getValue
      )
    )

  override protected def entityToDocument(entity: RecommendationsResponse): Document = {
    val document = Document(
      "email" -> BsonString(entity.email),
      "recommendationId" -> BsonString(entity.recommendationId),
      "upVote" -> BsonBoolean(entity.upVote),
      "downVote" -> BsonBoolean(entity.downVote)
    )
    document
  }
}

// $COVERAGE-OFF$
