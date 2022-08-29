package com.knoldus.routes.contract.feedbackform

import java.time.temporal.ChronoUnit
import java.time.{ LocalDate, ZoneId }

import com.knoldus.routes.contract.user.UserResponse
import play.api.libs.json.{ Json, OWrites }

final case class BannedUser(bannedDaysLeft: Long, bannedTill: String)

object BannedUser {

  implicit val BannedUserResponseWrites: OWrites[BannedUser] = Json.writes[BannedUser]

  def fromDomain(banUserInfo: UserResponse): BannedUser = {
    val userBanTillDate = banUserInfo.banTill.toString
    val instant = banUserInfo.banTill.toInstant
    val localBanDate = instant.atZone(ZoneId.systemDefault()).toLocalDate
    val startDate = LocalDate.now
    val daysLeft = startDate.until(localBanDate, ChronoUnit.DAYS)

    BannedUser(daysLeft, userBanTillDate)
  }

}
