package com.knoldus.domain.session

import com.knoldus.routes.contract.session.PresenterDetails

final case class NewSession(
  presenterDetails: PresenterDetails,
  coPresenterDetails: Option[PresenterDetails],
  dateTime: Long,
  sessionDuration: Int,
  topic: String,
  category: String,
  subCategory: String,
  feedbackFormId: Option[String],
  feedbackExpirationDate: Long,
  sessionType: String,
  sessionState: String,
  sessionDescription: String,
  youtubeURL: Option[String],
  slideShareURL: Option[String],
  slideURL: Option[String],
  slidesApprovedBy: Option[String],
  sessionApprovedBy: Option[String],
  sessionTag: List[String],
  remarks: Option[String]
)

object sessionState extends Enumeration {
  type sessionState = Value
  val PENDINGFORUSER = Value("PendingForUser")
  val PENDINGFORADMIN = Value("PendingForAdmin")
  val APPROVED = Value("Approved")
  val REJECTED = Value("Rejected")
  val COMPLETED = Value("Completed")

}
