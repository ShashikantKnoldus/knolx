package com.knoldus.services.slot

import akka.Done
import akka.event.LoggingAdapter
import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.And
import com.knoldus.dao.holiday.HolidayDao
import com.knoldus.dao.holiday.HolidayDao.HolidayDate
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.dao.slot.SlotDao
import com.knoldus.dao.slot.SlotDao.{ EndDateTime, StartDateTime }
import com.knoldus.domain.session.{ GetSlotsResponse, SessionDetails }
import com.knoldus.domain.slot.{ Slot, SlotType, ValidatedSlotDetail }
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.services.slot.SlotService.SlotServiceError
import com.typesafe.config.Config

import java.time.temporal.TemporalAdjusters
import java.time._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps

class SlotService(conf: Config, slotDao: SlotDao, newSessionDao: NewSessionDao, holidayDao: HolidayDao)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {

  val slotDurations: Map[String, Int] =
    SlotType.validValues.map(st => st.toString -> conf.getInt(s"slotDurations.${st.toString}")).toMap

  def createSlot(slotType: String, dateTime: Long)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[SlotServiceError, WithId[Slot]]] =
    if (authorityUser.keycloakRole == Admin)
      validateSlotDetails(slotType, dateTime) match {
        case Invalid(errors) => Future.successful(SlotServiceError.InvalidSlotDetails(errors).asLeft)
        case Valid(_) =>
          val slot = Slot(
            slotType = slotType,
            dateTime = dateTime,
            bookable = true,
            createdBy = authorityUser.email,
            createdOn = System.currentTimeMillis(),
            sessionId = None,
            slotDuration = slotDurations(slotType)
          )
          slotDao.create(slot).map { id =>
            val slotInfo = WithId(slot, id)
            slotInfo.asRight
          }
      }
    else Future.successful(SlotServiceError.AccessDenied.asLeft)

  def deleteSlot(id: String)(implicit authorityUser: NewUserInformation): Future[Either[SlotServiceError, Done]] =
    if (authorityUser.keycloakRole == Admin)
      slotDao.get(id).flatMap {
        case Some(WithId(slot, id)) =>
          slot.sessionId match {
            case Some(_) => Future.successful(SlotServiceError.SlotBookedError.asLeft)
            case None => slotDao.delete(id).map(_ => Done.asRight)
          }
        case None =>
          Future.successful(SlotServiceError.SlotNotFoundError.asLeft)
      }
    else
      Future.successful(SlotServiceError.AccessDenied.asLeft)

  def updateSlot(
    slotId: String,
    newSlotType: String,
    newDateTime: Long
  )(implicit authorityUser: NewUserInformation): Future[Either[SlotServiceError, Done]] =
    if (authorityUser.keycloakRole == Admin)
      validateSlotDetails(newSlotType, newDateTime) match {
        case Invalid(errors) => Future.successful(SlotServiceError.InvalidSlotDetails(errors).asLeft)
        case Valid(_) =>
          slotDao.get(slotId).flatMap {
            case None =>
              Future.successful(SlotServiceError.SlotNotFoundError.asLeft)
            case Some(WithId(slot, id)) =>
              val updatedSlot = slot.copy(
                slotType = newSlotType,
                dateTime = newDateTime,
                slotDuration = slotDurations(newSlotType)
              )
              slotDao.update(id, _ => updatedSlot).map {
                case Some(_) => Done.asRight
                case None => SlotServiceError.SlotNotUpdatedError.asLeft
              }
          }
      }
    else
      Future.successful(SlotServiceError.AccessDenied.asLeft)

  def getSlotsInMonth: Future[Seq[GetSlotsResponse]] = {
    val todayDate = LocalDateTime.now()
    val setStartDate = todayDate.`with`((TemporalAdjusters.firstDayOfMonth()))
    val setEndDate = todayDate.`with`((TemporalAdjusters.firstDayOfMonth())).plusMonths(4).minusDays(1)
    val zonedStartDateTime = ZonedDateTime.of(setStartDate, ZoneId.systemDefault)
    val startDate = zonedStartDateTime.toInstant.toEpochMilli
    val zonedEndDateTime = ZonedDateTime.of(setEndDate, ZoneId.systemDefault)
    val endDate = zonedEndDateTime.toInstant.toEpochMilli

    val slot: Future[Seq[WithId[Slot]]] = for {
      result <- slotDao.listAll(
        And(StartDateTime(startDate), EndDateTime(endDate))
      )
    } yield result
    slot.flatMap { seqWithIdSlot =>
      Future.sequence(seqWithIdSlot.map { withIdSlot =>
        withIdSlot.entity.sessionId match {
          case Some(sessionId) =>
            newSessionDao.get(sessionId).map {
              case Some(withIdSession) =>
                GetSlotsResponse(
                  withIdSlot.id,
                  withIdSlot.entity.dateTime,
                  withIdSlot.entity.bookable,
                  withIdSlot.entity.createdBy,
                  Some(
                    SessionDetails(
                      withIdSession.id,
                      withIdSession.entity.topic,
                      withIdSession.entity.presenterDetails,
                      withIdSession.entity.coPresenterDetails
                    )
                  ),
                  withIdSlot.entity.slotDuration,
                  withIdSlot.entity.slotType
                )
              case None =>
                GetSlotsResponse(
                  withIdSlot.id,
                  withIdSlot.entity.dateTime,
                  withIdSlot.entity.bookable,
                  withIdSlot.entity.createdBy,
                  None,
                  withIdSlot.entity.slotDuration,
                  withIdSlot.entity.slotType
                )
            }
          case None =>
            Future.successful(
              GetSlotsResponse(
                withIdSlot.id,
                withIdSlot.entity.dateTime,
                withIdSlot.entity.bookable,
                withIdSlot.entity.createdBy,
                None,
                withIdSlot.entity.slotDuration,
                withIdSlot.entity.slotType
              )
            )
        }
      })

    }
  }

  def getKnolmeetStartAndEndDateTime(slotDate: LocalDate): (LocalDateTime, LocalDateTime) = {
    val lastFridayOfMonth = slotDate.`with`(TemporalAdjusters.lastInMonth(DayOfWeek.FRIDAY))
    val isHolidayPresent = holidayDao.get(HolidayDate(lastFridayOfMonth.toString))
    val result = isHolidayPresent.map {
      case Some(_) => lastFridayOfMonth.minusDays(7)
      case None => lastFridayOfMonth
    }
    val knolmeetDate = Await.result(result, 500 millis)
    val (knolmeetStartTime, knolmeetEndTime) = (LocalTime.of(12, 0), LocalTime.of(13, 15))
    val (knolmeetStartDateTime, knolmeetEndDateTime) =
      (LocalDateTime.of(knolmeetDate, knolmeetStartTime), LocalDateTime.of(knolmeetDate, knolmeetEndTime))
    (knolmeetStartDateTime, knolmeetEndDateTime)
  }

  def isConflictWithKnolmeet(slotDateTimeInMillis: Long): Boolean = {
    val slotDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(slotDateTimeInMillis), ZoneId.systemDefault())
    val slotDate = slotDateTime.toLocalDate
    val (knolmeetStartDateTime, knolmeetEndDateTime) = getKnolmeetStartAndEndDateTime(slotDate)

    if (
      slotDateTime.isEqual(knolmeetStartDateTime) || (slotDateTime.isAfter(knolmeetStartDateTime) && slotDateTime
        .isBefore(knolmeetEndDateTime))
    ) true
    else false
  }

  def validateSlotDetails(slotType: String, dateTime: Long): Validated[Seq[String], ValidatedSlotDetail] = {
    logger.info("Validating slot details.")
    (
      if (!SlotType.validateSlotType(slotType).equals(SlotType.INVALID_SlOT))
        SlotType.withName(slotType).valid
      else Seq("Invalid Slot Type").invalid,
      if (dateTime > System.currentTimeMillis())
        dateTime.valid
      else Seq("Date time should not in past").invalid,
      if (!slotType.equals(SlotType.KNOLMEET.toString))
        if (!isConflictWithKnolmeet(dateTime))
          false.valid
        else Seq("Provided date time is conflicting with Knolmeet date time").invalid
      else false.valid
    ).mapN(ValidatedSlotDetail)
  }
}

object SlotService {

  sealed trait SlotServiceError

  object SlotServiceError {

    case object AccessDenied extends SlotServiceError

    case object SlotNotFoundError extends SlotServiceError

    case object SlotBookedError extends SlotServiceError

    case object SlotNotCreatedError extends SlotServiceError

    case object EmailNotFoundError extends SlotServiceError

    case object SlotNotUpdatedError extends SlotServiceError

    case class InvalidSlotDetails(errors: Seq[String]) extends SlotServiceError
  }
}
