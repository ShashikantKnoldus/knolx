package com.knoldus.routes.contract.session

import java.util.Date
import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Session
import ai.x.play.json.Encoders.encoder
import ai.x.play.json.Jsonx
import play.api.libs.json.Format

final case class GetSessionByIdResponse(
  userId: String,
  email: String,
  fullName: String,
  date: Date,
  session: String,
  category: String,
  subCategory: String,
  feedbackFormId: String,
  topic: String,
  brief: String,
  feedbackExpirationDays: Int,
  meetup: Boolean,
  rating: String,
  score: Double,
  cancelled: Boolean,
  active: Boolean,
  expirationDate: Date,
  youtubeURL: String,
  slideShareURL: String,
  temporaryYoutubeURL: Option[String],
  reminder: Boolean,
  notification: Boolean,
  id: String
)

object GetSessionByIdResponse {

  implicit val GetSessionByIdResponseWrites: Format[GetSessionByIdResponse] =
    Jsonx.formatCaseClass[GetSessionByIdResponse]

  def fromDomain(session: WithId[Session]): GetSessionByIdResponse =
    session match {
      case WithId(entity, id) =>
        val youtubeURL =
          entity.youtubeURL

        val fullName = entity.email
          .split("@")
          .headOption
          .fold("Invalid") { name =>
            name.split('.').map(_.capitalize).mkString(" ")
          }
          .trim

        GetSessionByIdResponse(
          entity.userId,
          entity.email,
          fullName,
          entity.date,
          entity.session,
          entity.category,
          entity.subCategory,
          entity.feedbackFormId,
          entity.topic,
          entity.brief,
          entity.feedbackExpirationDays,
          entity.meetup,
          entity.rating,
          entity.score,
          entity.cancelled,
          entity.active,
          entity.expirationDate,
          youtubeURL.fold("")(identity),
          entity.slideShareURL.fold("")(identity),
          entity.temporaryYoutubeURL,
          entity.reminder,
          entity.notification,
          id
        )
    }
}
