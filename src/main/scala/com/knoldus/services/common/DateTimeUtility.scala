package com.knoldus.services.common

import java.text.SimpleDateFormat
import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime, ZoneId, ZoneOffset }
import java.util.{ Date, TimeZone }

class DateTimeUtility {

  val ISTZoneId: ZoneId = ZoneId.of("Asia/Kolkata")
  val ISTTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
  val ZoneOffset: ZoneOffset = ISTZoneId.getRules.getOffset(LocalDateTime.now(ISTZoneId))
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  val dateFormatWithT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
  val yearMonthFormatDB = new SimpleDateFormat("yyyy-MM")
  val yearMonthFormat = new SimpleDateFormat("yyyy-MMMM")

  def nowMillis: Long =
    System.currentTimeMillis

  def toLocalDateTimeEndOfDay(date: Date): LocalDateTime =
    Instant.ofEpochMilli(date.getTime).atZone(ISTZoneId).toLocalDateTime.`with`(LocalTime.MAX)

  def toMillis(localDateTime: LocalDateTime): Long =
    localDateTime.toEpochSecond(ZoneOffset) * 1000

  def parseDateStringToIST(date: String): Long = {
    val millis = dateFormat.parse(date).getTime
    Instant.ofEpochMilli(millis).atZone(ISTZoneId).toLocalDateTime.toEpochSecond(ZoneOffset) * 1000
  }

  def toLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ISTZoneId).toLocalDate

  def localDateIST: LocalDate =
    LocalDate.now(ISTZoneId)

  def endOfDayMillis: Long =
    localDateIST.atTime(23, 59, 59).toEpochSecond(ZoneOffset) * 1000

  def yearMonthFormat(date: Date): String =
    yearMonthFormat.format(date)

  def localDateTimeIST: LocalDateTime =
    LocalDateTime.now(ISTZoneId)

  def toLocalDateTime(millis: Long): LocalDateTime =
    Instant.ofEpochMilli(millis).atZone(ISTZoneId).toLocalDateTime
}
