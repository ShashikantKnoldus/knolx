package com.knoldus.services.usermanagement.utilities

import org.mindrot.jbcrypt

// $COVERAGE-OFF$

class PasswordUtility {

  val BCrypt = "BCrypt"

  def encrypt(password: String): String =
    jbcrypt.BCrypt.hashpw(password, jbcrypt.BCrypt.gensalt(10))

  def isPasswordValid(enteredPassword: String, savedPassword: String): Boolean =
    jbcrypt.BCrypt.checkpw(enteredPassword, savedPassword)

}
// $COVERAGE-ON$
