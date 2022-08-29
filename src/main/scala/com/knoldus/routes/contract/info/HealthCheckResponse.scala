package com.knoldus.routes.contract.info

import play.api.libs.json.{ Json, OWrites }

final case class HealthCheckResponse(isHealthy: Boolean)

object HealthCheckResponse {

  implicit val HealthCheckResponseWrites: OWrites[HealthCheckResponse] = Json.writes[HealthCheckResponse]

}
