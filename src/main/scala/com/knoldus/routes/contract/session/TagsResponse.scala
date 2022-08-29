package com.knoldus.routes.contract.session

import play.api.libs.json.{ Json, OWrites }

case class TagsResponse(tags: Seq[String])

object TagsResponse {
  implicit val tagsResponseWrites: OWrites[TagsResponse] = Json.writes[TagsResponse]
}
