package com.knoldus.routes.contract.feedbackform

import play.api.libs.json.{ Json, OWrites }

final case class ReportResult(feedbackReportHeaderList: List[FeedbackReportHeader], pageNumber: Int, pages: Int)

object ReportResult {
  implicit val FeedbackFormReportHeaderWrites: OWrites[FeedbackReportHeader] = Json.writes[FeedbackReportHeader]
  implicit val FeedbackFormReportWrites: OWrites[ReportResult] = Json.writes[ReportResult]

  def fromDomain(reportResult: ReportResult): ReportResult =
    ReportResult(reportResult.feedbackReportHeaderList, reportResult.pageNumber, reportResult.pages)
}
