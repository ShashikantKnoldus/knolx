package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.NewSession
import play.api.libs.json.{ Json, OWrites }

final case class SessionStatusResponse(knolx: Seq[KnolxSession], count: Int, pages: Int)

object SessionStatusResponse {

  implicit val SessionStatusResponseWrites: OWrites[SessionStatusResponse] = Json.writes[SessionStatusResponse]

  def fromDomain(session: Seq[WithId[NewSession]], count: Int, pageSize: Int): SessionStatusResponse = {

    val knolx = session.map { sessionInfo =>
      val contentStatus =
        if (sessionInfo.entity.sessionState == "Cancelled")
          ContentStatus.Cancelled
        else if (sessionInfo.entity.youtubeURL.isDefined || sessionInfo.entity.slideShareURL.isDefined)
          ContentStatus.Available
        else
          ContentStatus.NotAvailable

      val contentInformation = ContentInformation(
        contentStatus,
        sessionInfo.entity.slideShareURL,
        sessionInfo.entity.youtubeURL
      )

      KnolxSession(
        sessionInfo.id,
        sessionInfo.entity.presenterDetails,
        sessionInfo.entity.coPresenterDetails,
        sessionInfo.entity.dateTime,
        sessionInfo.entity.sessionDuration,
        sessionInfo.entity.topic,
        sessionInfo.entity.category,
        sessionInfo.entity.subCategory,
        sessionInfo.entity.feedbackFormId,
        sessionInfo.entity.feedbackExpirationDate,
        sessionInfo.entity.sessionType,
        sessionInfo.entity.sessionState,
        sessionInfo.entity.sessionDescription,
        sessionInfo.entity.youtubeURL.isDefined || sessionInfo.entity.slideShareURL.isDefined,
        contentInformation,
        sessionInfo.entity.slidesApprovedBy,
        sessionInfo.entity.sessionApprovedBy
      )
    }
    val pages = Math.ceil(count.toDouble / pageSize).toInt
    SessionStatusResponse(
      knolx,
      count,
      pages
    )
  }

}
