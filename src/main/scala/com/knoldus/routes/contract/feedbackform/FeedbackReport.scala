package com.knoldus.routes.contract.feedbackform

import play.api.libs.json.{ Json, OWrites }

final case class FeedbackReport(reportHeader: Option[FeedbackReportHeader], response: List[UserFeedbackReport])

object FeedbackReport {
  implicit val FeedbackFormReportHeaderWrites: OWrites[FeedbackReportHeader] = Json.writes[FeedbackReportHeader]
  implicit val UserFeedbackHeaderWrites: OWrites[UserFeedbackReport] = Json.writes[UserFeedbackReport]
  implicit val FeedbackReportWrites: OWrites[FeedbackReport] = Json.writes[FeedbackReport]

  def fromDomain(feedbackReport: FeedbackReport): FeedbackReport =
    FeedbackReport(feedbackReport.reportHeader, feedbackReport.response)

}
