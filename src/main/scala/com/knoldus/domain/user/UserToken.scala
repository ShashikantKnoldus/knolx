package com.knoldus.domain.user

import java.util.Date

final case class UserToken(token: String, expireOn: Date, userInformation: UserInformation)
