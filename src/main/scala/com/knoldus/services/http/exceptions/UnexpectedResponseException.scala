package com.knoldus.services.info.http.exceptions

import akka.http.scaladsl.model.{ HttpEntity, HttpRequest, StatusCode }

final case class UnexpectedResponseException(
  request: HttpRequest,
  expectedCodes: Seq[StatusCode],
  actualCode: StatusCode,
  entity: HttpEntity.Strict
) extends RuntimeException(
      s"Unexpected response code for request $request." +
          s"$expectedCodes. Actual: $actualCode. Response entity: ${entity.getData.utf8String}"
    )
