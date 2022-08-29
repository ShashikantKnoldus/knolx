package com.knoldus.domain.user

import java.util.Date

final case class UserInformation(
  email: String,
  active: Boolean,
  admin: Boolean,
  coreMember: Boolean,
  superUser: Boolean,
  banTill: Date,
  banCount: Int = 0,
  lastBannedOn: Option[Date],
  nonParticipating: Boolean,
  department: Option[String]
) {

  def isSuperAdmin: Boolean =
    superUser

  def isAdmin: Boolean =
    admin && !superUser

  def isAtLeastAdmin: Boolean = isSuperAdmin || isAdmin

}
