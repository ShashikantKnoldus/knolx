package com.knoldus.routes.info

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.routes.BaseRoutes
import com.knoldus.routes.contract.info.{ HealthCheckResponse, StatusResponse }
import com.knoldus.services.info.InfoService

class InfoRoutes(service: InfoService) extends BaseRoutes {

  val routes: Route =
    concat(
      path("status") {
        get {
          onSuccess(service.getUptime) { status =>
            complete(StatusResponse.fromDomain(status))
          }
        }
      },
      path("health") {
        get {
          val dbStatus = service.getDbStatus
          complete(HealthCheckResponse(isHealthy = dbStatus))
        }
      }
    )
}
