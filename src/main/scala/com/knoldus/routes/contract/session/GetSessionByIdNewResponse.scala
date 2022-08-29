package com.knoldus.routes.contract.session

import ai.x.play.json.Encoders.encoder
import ai.x.play.json.Jsonx
import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.NewSession
import play.api.libs.json.Format

final case class GetSessionByIdNewResponse(
  id: String,
  presenterDetail: PresenterDetails,
  coPresenterDetails: Option[PresenterDetails],
  dateTime: Long,
  sessionDurationInMins: Long,
  topic: String,
  category: String,
  subCategory: String,
  feedbackFormId: Option[String],
  feedbackExpriationDate: Long,
  sessionType: String,
  sessionState: String,
  sessionDescription: String,
  youtubeURL: String,
  slideshareURL: String,
  slideURL: String,
  sessionTag: List[String],
  slidesApprovedBy: Option[String],
  sessionApprovedBy: Option[String],
  remarks: Option[String]
)

object GetSessionByIdNewResponse {

  implicit val GetSessionByIdNewResponseWrites: Format[GetSessionByIdNewResponse] =
    Jsonx.formatCaseClass[GetSessionByIdNewResponse]

  def fromDomain(session: WithId[NewSession]): GetSessionByIdNewResponse =
    session match {
      case WithId(entity, id) =>
        val youtubeURL =
          entity.youtubeURL
        GetSessionByIdNewResponse(
          id,
          entity.presenterDetails,
          entity.coPresenterDetails,
          entity.dateTime,
          entity.sessionDuration,
          entity.topic,
          entity.category,
          entity.subCategory,
          entity.feedbackFormId,
          entity.feedbackExpirationDate,
          entity.sessionType,
          entity.sessionState,
          entity.sessionDescription,
          youtubeURL.fold("")(identity),
          entity.slideShareURL.fold("")(identity),
          entity.slideURL.fold("")(identity),
          entity.sessionTag,
          entity.slidesApprovedBy,
          entity.sessionApprovedBy,
          entity.remarks
        )
    }
}
