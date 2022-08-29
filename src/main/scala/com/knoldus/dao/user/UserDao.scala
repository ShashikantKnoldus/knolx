package com.knoldus.dao.user

import java.util.Date

import com.knoldus.dao.feedbackform.FeedbackFormDao.Name
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.filters.Filters.{ NameIs, SearchQuery }
import com.knoldus.dao.mongo.MongoEntityDao
import com.knoldus.dao.sorting.Field
import com.knoldus.dao.user.UserDao._
import com.knoldus.domain.user.UserInformation
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{ Document, MongoDatabase }
import com.knoldus.dao.mongo.utils.BsonHelpers._
import org.mongodb.scala.bson.{ BsonBoolean, BsonDateTime, BsonInt32, BsonString }

import scala.util.Try

// $COVERAGE-OFF$
object UserDao {

  final case class CheckBanUser(date: Date) extends Filter

  final case class CoreMember(coreMember: Boolean) extends Filter

  final case class ActiveEmailCheck(email: String) extends Filter

  final case class SuspendedEmailCheck(email: String) extends Filter

  final case class EmailCheck(email: String) extends Filter

  final case class EmailFilter(email: String) extends Filter

  final case class Admin(admin: Boolean) extends Filter

  final case class CheckUnBanUser(date: Date) extends Filter

  final case class SuperUser(admin: Boolean) extends Filter

  final case class CheckActiveUsers(active: Boolean) extends Filter

  final case class UserDepartmentCheck(name: String) extends Filter

  case object Name extends Field

  case object EmailField extends Field

  final case class NonParticipatingUsers(nonParticipating: Boolean) extends Filter

}

class UserDao(protected val database: MongoDatabase) extends MongoEntityDao[UserInformation] {
  override val collectionName: String = "users"

  override protected val fieldMapper: Map[Field, String] = {
    Map(Name -> "name")
    Map(EmailField -> "email")
  }

  override protected val specificFilterMapper: PartialFunction[Filter, Try[Bson]] = {
    case NameIs(name) => Try(equal("email", name))
    case SearchQuery(email) => Try(equal("email", email))
    case CoreMember(coreMember) => Try(equal("coreMember", coreMember))
    case CheckBanUser(date) => Try(gt("banTill", date))
    case CheckUnBanUser(date) => Try(lt("banTill", date))
    case EmailCheck(email) => Try(equal("email", email))
    case Admin(admin) => Try(equal("admin", admin))
    case SuperUser(superUser) => Try(equal("superUser", superUser))
    case ActiveEmailCheck(email) => Try(and(equal("email", email), equal("active", true)))
    case SuspendedEmailCheck(email) => Try(and(equal("email", email), equal("active", false)))
    case CheckActiveUsers(active) => Try(equal("active", active))
    case EmailFilter(email) => Try(regex("email", email))
    case NonParticipatingUsers(nonParticipating) => Try(equal("nonParticipating", nonParticipating))
    case UserDepartmentCheck(department) => Try(equal("department", department))
  }

  override protected def documentToEntity(document: Document): Try[UserInformation] =
    Try(
      UserInformation(
        email = document.getMandatory[BsonString]("email").getValue,
        active = document.getMandatory[BsonBoolean]("active").getValue,
        admin = document.getMandatory[BsonBoolean]("admin").getValue,
        coreMember = document.getMandatory[BsonBoolean]("coreMember").getValue,
        superUser = document.getMandatory[BsonBoolean]("superUser").getValue,
        banTill = new Date(document.getMandatory[BsonDateTime]("banTill").getValue),
        banCount = document.getMandatory[BsonInt32]("banCount").getValue,
        lastBannedOn = document.get[BsonDateTime]("lastBannedOn").map(value => new Date(value.getValue)),
        nonParticipating = document.getMandatory[BsonBoolean]("nonParticipating").getValue,
        department = document.get[BsonString]("department").map(_.getValue)
      )
    )

  override protected def entityToDocument(entity: `UserInformation`): Document =
    Document(
      "email" -> BsonString(entity.email),
      "active" -> BsonBoolean(entity.active),
      "admin" -> BsonBoolean(entity.admin),
      "coreMember" -> BsonBoolean(entity.coreMember),
      "superUser" -> BsonBoolean(entity.superUser),
      "banTill" -> BsonDateTime(entity.banTill),
      "banCount" -> BsonInt32(entity.banCount),
      "lastBannedOn" -> entity.lastBannedOn.map(BsonDateTime(_)),
      "nonParticipating" -> BsonBoolean(entity.nonParticipating),
      "department" -> entity.department.map(BsonString(_))
    )

}

// $COVERAGE-ON$
