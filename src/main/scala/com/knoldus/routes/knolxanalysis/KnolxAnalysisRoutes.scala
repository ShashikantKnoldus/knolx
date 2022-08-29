package com.knoldus.routes.knolxanalysis

import java.util.Date
import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.routes.BaseRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.knolxanalysis.{ CategoryAnalysisResponse, SubCategoryAnalysisResponse }
import com.knoldus.services.knolxanalysis.KnolxAnalysisService
import com.knoldus.services.knolxanalysis.KnolxAnalysisService.KnolxAnalysisServiceError

class KnolxAnalysisRoutes(service: KnolxAnalysisService) extends BaseRoutes {

  val routes: Route =
    pathPrefix("knolxanalysis") {
      path("columnchart") {
        (get & parameters("startDate".as[String], "endDate".as[String])) { (startDate, endDate) =>
          onSuccess(service.getSessionInRange(startDate, endDate)) {
            case Right(sessions) => complete(SubCategoryAnalysisResponse.fromDomain(sessions))
            case Left(error) => complete(translateError(error))
          }
        }
      } ~
        path("piechart") {
          (get & parameters("startDate".as[String], "endDate".as[String])) { (startDate, endDate) =>
            onSuccess(service.doCategoryAnalysis(startDate, endDate)) {
              case Right(result) =>
                val (categoryInfo, totalSessions) = result
                complete(CategoryAnalysisResponse.fromDomain(totalSessions, categoryInfo))
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path("linechart") {
          (get & parameters("startDate".as[String], "endDate".as[String])) { (startDate, endDate) =>
            onSuccess(service.doKnolxMonthlyAnalysis(startDate, endDate)) {
              case Right(result) =>
                complete(result)
              case Left(error) => complete(translateError(error))
            }
          }
        } ~
        path("session-in-time-range") {
          (put & parameters("email".as[String].?, "startDate".as[Long], "endDate".as[Long])) {
            (email, startDate, endDate) =>
              onSuccess(service.sessionsInTimeRange(email, new Date(startDate), new Date(endDate))) {
                case Right(session) => complete(session)
                case Left(error) => complete(translateError(error))
              }
          }
        }
    }

  def translateError(error: KnolxAnalysisServiceError): (StatusCode, ErrorResponse) =
    error match {

      case KnolxAnalysisServiceError.SessionsNotFoundError =>
        errorResponse(StatusCodes.NotFound, "No Sessions Found")
    }
}
