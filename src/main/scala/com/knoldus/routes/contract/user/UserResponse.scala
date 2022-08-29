package com.knoldus.routes.contract.user

import java.util.Date

import com.knoldus.dao.commons.WithId
import com.knoldus.domain.user.UserInformation
import play.api.libs.json.{ Json, OWrites }

final case class UserResponse(
  email: String,
  active: Boolean,
  admin: Boolean,
  coreMember: Boolean,
  superUser: Boolean,
  banTill: Date,
  banCount: Int = 0,
  department: Option[String]
)

object UserResponse {
  implicit val UserResponseWrites: OWrites[UserResponse] = Json.writes[UserResponse]

  def fromDomain(userInfo: WithId[UserInformation]): Map[String, Int] = {
    val userData = userInfo.entity
    Map("banCount" -> userData.banCount)
  }
}
