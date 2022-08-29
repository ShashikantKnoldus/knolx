package com.knoldus.routes.contract.info

import com.knoldus.domain.info.Status
import com.knoldus.utils.json.CommonFormats.DurationFormat
import play.api.libs.json.{ Json, OWrites }

import scala.concurrent.duration.Duration

final case class StatusResponse(uptime: Duration, apiVersion: String)

object StatusResponse {

  implicit val StatusResponseWrites: OWrites[StatusResponse] = Json.writes[StatusResponse]

  def fromDomain(status: Status): StatusResponse =
    StatusResponse(
      uptime = status.uptime,
      apiVersion = status.apiVersion
    )

}
