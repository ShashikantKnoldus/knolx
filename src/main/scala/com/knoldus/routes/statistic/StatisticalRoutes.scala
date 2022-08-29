package com.knoldus.routes.statistic

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.statistic.StatisticResponse
import com.knoldus.services.statistic.StatisticalService
import com.knoldus.services.statistic.StatisticalService.StatisticalServiceError
import com.knoldus.services.statistic.StatisticalService.StatisticalServiceError.InvalidDates
import com.knoldus.services.usermanagement.AuthorizationService

class StatisticalRoutes(statisticalService: StatisticalService, val authorizationService: AuthorizationService)
    extends AuthorizationRoutes {

  val routes: Route =
    path("statistic") {
      (get & parameters("startDate".as[Long], "endDate".as[Long])) { (startDate, endDate) =>
        onSuccess(statisticalService.getKnolxDetails(Some(startDate), Some(endDate))) {
          case Right(response) => complete(StatisticResponse.fromDomain(response))
          case Left(error) => complete(translateError(error))
        }
      } ~
        pathEnd {
          get {
            onSuccess(statisticalService.getKnolxDetails()) {
              case Right(response) => complete(StatisticResponse.fromDomain(response))
              case Left(error) => complete(translateError(error))
            }
          }
        }
    }

  def translateError(error: StatisticalServiceError): (StatusCode, ErrorResponse) =
    error match {
      case InvalidDates => errorResponse(StatusCodes.BadRequest, "Start date should be before end date")
    }

}
