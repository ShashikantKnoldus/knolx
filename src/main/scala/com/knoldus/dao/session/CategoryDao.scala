package com.knoldus.dao.session

import com.knoldus.dao.filters.Filter
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.mongo.utils.BsonHelpers._
import com.knoldus.dao.session.CategoryDao.{ CategoryName, ListAll }
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.session.Category
import org.mongodb.scala.bson.{ BsonArray, BsonString }
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{ Document, MongoDatabase }

import scala.util.Try

// $COVERAGE-OFF$
object CategoryDao {
  case object CategoryName extends Field
  final case class CategoryName(name: String) extends Filter
  final case class ListAll(term: String) extends Filter

}

class CategoryDao(protected val database: MongoDatabase) extends MongoEntityDao[Category] {
  override val collectionName: String = "categories"

  override protected val fieldMapper: Map[Field, String] =
    Map(CategoryName -> "categoryName")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case CategoryName(name) => Try(equal("categoryName", name))
    case ListAll(term) => Try(gte("categoryName", term))
  }

  override protected def documentToEntity(document: Document): Try[Category] =
    Try(
      Category(
        categoryName = document.getMandatory[BsonString]("categoryName").getValue,
        subCategory =
          document.getMandatory[BsonArray]("subCategory").map(subCategory => subCategory.asString().getValue)
      )
    )

  override protected def entityToDocument(entity: Category): Document =
    Document(
      "categoryName" -> BsonString(entity.categoryName),
      "subCategory" -> entity.subCategory.map(BsonString(_))
    )
}

// $COVERAGE-ON$
