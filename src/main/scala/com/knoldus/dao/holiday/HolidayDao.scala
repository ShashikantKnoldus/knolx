package com.knoldus.dao.holiday

import com.knoldus.dao.filters.Filter
import com.knoldus.dao.holiday.HolidayDao._
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.mongo.utils.BsonHelpers.DocumentExtensions
import com.knoldus.dao.sorting.Field
import com.knoldus.domain.holiday.Holiday
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{ Document, MongoDatabase }

import java.time.LocalDate
import scala.util.Try

object HolidayDao {
  case object HolidayDate extends Field
  final case class HolidayDate(date: String) extends Filter
}

class HolidayDao(protected val database: MongoDatabase) extends MongoEntityDao[Holiday] {
  override val collectionName: String = "holidays"
  override protected val fieldMapper: Map[Field, String] = Map(HolidayDate -> "date")

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case HolidayDate(date) => Try(equal("date", date))
  }

  override protected def entityToDocument(entity: Holiday): Document =
    Document(
      "date" -> BsonString(entity.date),
      "occasion" -> BsonString(entity.occasion)
    )

  override protected def documentToEntity(document: Document): Try[Holiday] =
    Try(
      Holiday(
        date = document.getMandatory[BsonString]("date").getValue,
        occasion = document.getMandatory[BsonString]("occasion").getValue
      )
    )
}
