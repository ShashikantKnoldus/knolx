package com.knoldus.routes.contract.session

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.session.Session
import play.api.libs.json.{ Json, OWrites }

final case class CreateSessionResponse(id: String, email: String, category: String, subCategory: String, topic: String)

object CreateSessionResponse {
  implicit val CreateSessionResponseWrites: OWrites[CreateSessionResponse] = Json.writes[CreateSessionResponse]

  def fromDomain(session: WithId[Session]): CreateSessionResponse =
    session match {
      case WithId(entity, id) =>
        CreateSessionResponse(
          id,
          entity.email,
          entity.category,
          entity.subCategory,
          entity.topic
        )
    }
}
