package com.knoldus.services.session

import akka.Done
import akka.event.LoggingAdapter
import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import cats.implicits.{ catsSyntaxEitherId, catsSyntaxValidatedId }
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, Or }
import com.knoldus.dao.session.NewSessionDao
import com.knoldus.dao.session.NewSessionDao.{
  CoPresenter,
  DateMatch,
  DateTime,
  MatchCoPresenter,
  MatchPresenter,
  Presenter,
  SessionState,
  SessionsExcept,
  TopicMatch,
  Upcoming
}
import com.knoldus.dao.slot.SlotDao
import com.knoldus.dao.sorting.Direction.Descending
import com.knoldus.dao.sorting.SortBy
import com.knoldus.domain.session.{ sessionState, NewSession }
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.session.{ BookSessionUserRequest, PresenterDetails }
import com.knoldus.services.common.DateTimeUtility
import com.knoldus.services.session.NewSessionService.NewSessionServiceError
import com.google.api.services.directory.Directory
import com.knoldus.domain.session.sessionState.{ APPROVED, PENDINGFORADMIN }
import com.knoldus.utils.Base64

import java.io.IOException
import java.security.GeneralSecurityException
import java.util.Date
import java.util.regex.Pattern
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

class NewSessionService(slotDao: SlotDao, newSessionDao: NewSessionDao, service: Directory)(implicit
  ec: ExecutionContext,
  logger: LoggingAdapter
) {

  val dateTimeUtility = new DateTimeUtility

  def getNewSession(
    sessionId: String
  )(implicit authorityUser: NewUserInformation): Future[Either[NewSessionServiceError, WithId[NewSession]]] = {
    val sessions = newSessionDao.get(sessionId)
    sessions.map {
      case Some(id) =>
        id.asRight
      case None =>
        logger.error(s"Error in getting sessions")
        NewSessionServiceError.SessionNotFound.asLeft
    }
  }

  def getRequestedSessions(
    pageNumber: Int,
    pageSize: Int,
    search: Option[String] = None
  )(implicit userDetails: NewUserInformation): Future[Either[NewSessionServiceError, (Seq[WithId[NewSession]], Int)]] =
    if (userDetails.isAdmin)
      search match {
        case Some(key) =>
          val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
          val result = for {
            sessions <- newSessionDao.list(
              And(
                And(SessionState(PENDINGFORADMIN.toString), Upcoming(new Date().getTime)),
                Or(
                  Or(
                    MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                    MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                  ),
                  TopicMatch(emailAndTopicSearch)
                )
              ),
              pageNumber,
              pageSize,
              Some(SortBy((DateMatch, Descending)))
            )
            count <- newSessionDao.count(
              And(
                And(SessionState(PENDINGFORADMIN.toString), Upcoming(new Date().getTime)),
                Or(
                  Or(
                    MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                    MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                  ),
                  TopicMatch(emailAndTopicSearch)
                )
              )
            )
          } yield (sessions, count)
          result.map { response =>
            response.asRight
          }
        case None =>
          val result = for {
            sessions <- newSessionDao.list(
              And(SessionState(PENDINGFORADMIN.toString), Upcoming(new Date().getTime)),
              pageNumber,
              pageSize,
              Some(SortBy(fields = (DateMatch, Descending)))
            )
            count <- newSessionDao.count(And(SessionState(PENDINGFORADMIN.toString), Upcoming(new Date().getTime)))

          } yield (sessions, count)
          result.map { response =>
            response.asRight
          }
      }
    else
      Future.successful(NewSessionServiceError.AccessDenied.asLeft)

  @throws[IOException]
  @throws[GeneralSecurityException]
  def getUserProfilePicFromGoogleAPI(emailId: String): String =
    Try(service.users().photos().get(emailId).execute()) match {
      case Success(userPhoto) =>
        Base64.Encoder(userPhoto.decodePhotoData()).toBase64
      case Failure(exception) =>
        exception.printStackTrace()
        ""
    }

  def fetchSessionTags(keyword: String): Future[Either[NewSessionServiceError, Seq[String]]] =
    if (keyword.length < 2)
      Future.successful(Seq().asRight)
    else
      newSessionDao.fetchTags(keyword).map {
        case Some(tags) => tags.asRight
        case None => NewSessionServiceError.NotFound.asLeft
      }

  def getUpcomingSessions(
    pageNumber: Int,
    pageSize: Int,
    email: Option[String] = None
  ): Future[Either[NewSessionServiceError, (Seq[WithId[NewSession]], Int)]] =
    email match {
      case Some(key) =>
        val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
        val result = for {
          sessions <- newSessionDao.list(
            And(
              And(SessionsExcept("Cancelled"), Upcoming(new Date().getTime)),
              Or(
                Or(
                  MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                  MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                ),
                TopicMatch(emailAndTopicSearch)
              )
            ),
            pageNumber,
            pageSize,
            Some(SortBy((DateMatch, Descending)))
          )
          count <- newSessionDao.count(
            And(
              And(SessionsExcept("Cancelled"), Upcoming(new Date().getTime)),
              Or(
                Or(
                  MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                  MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                ),
                TopicMatch(emailAndTopicSearch)
              )
            )
          )
        } yield (sessions, count)
        result.map { response =>
          val (sessions, count) = response
          sessions.map { session =>
            val userPic = getUserProfilePicFromGoogleAPI(session.entity.presenterDetails.email)
            WithId(
              session.entity.copy(presenterDetails =
                PresenterDetails(
                  session.entity.presenterDetails.fullName,
                  session.entity.presenterDetails.email,
                  Some(userPic)
                )
              ),
              session.id
            )
          }
          (sessions, count).asRight
        }
      case None =>
        val result = for {
          sessions <- newSessionDao.list(
            And(SessionsExcept("Cancelled"), Upcoming(new Date().getTime)),
            pageNumber,
            pageSize,
            Some(SortBy(fields = (DateMatch, Descending)))
          )
          count <- newSessionDao.count(And(SessionsExcept("Cancelled"), Upcoming(new Date().getTime)))

        } yield (sessions, count)
        result.map { response =>
          val (sessions, count) = response
          val sessionsWithProfilePic = sessions.map { session =>
            val userProfilePic = getUserProfilePicFromGoogleAPI(session.entity.presenterDetails.email)
            val sessionInfo = session.entity.copy(presenterDetails =
              PresenterDetails(
                session.entity.presenterDetails.fullName,
                session.entity.presenterDetails.email,
                Some(userProfilePic)
              )
            )
            WithId(sessionInfo, session.id)
          }
          (sessionsWithProfilePic, count).asRight

        }
    }

  def getApprovedSessions(
    pageNumber: Int,
    pageSize: Int,
    search: Option[String] = None
  )(implicit userDetails: NewUserInformation): Future[Either[NewSessionServiceError, (Seq[WithId[NewSession]], Int)]] =
    if (userDetails.isAdmin)
      search match {
        case Some(key) =>
          val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
          val result = for {
            sessions <- newSessionDao.list(
              And(
                And(SessionState(APPROVED.toString), Upcoming(new Date().getTime)),
                Or(
                  Or(
                    MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                    MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                  ),
                  TopicMatch(emailAndTopicSearch)
                )
              ),
              pageNumber,
              pageSize,
              Some(SortBy((DateMatch, Descending)))
            )
            count <- newSessionDao.count(
              And(
                And(SessionState(APPROVED.toString), Upcoming(new Date().getTime)),
                Or(
                  Or(
                    MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                    MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                  ),
                  TopicMatch(emailAndTopicSearch)
                )
              )
            )
          } yield (sessions, count)
          result.map { response =>
            response.asRight
          }
        case None =>
          val result = for {
            sessions <- newSessionDao.list(
              And(SessionState(APPROVED.toString), Upcoming(new Date().getTime)),
              pageNumber,
              pageSize,
              Some(SortBy(fields = (DateMatch, Descending)))
            )
            count <- newSessionDao.count(And(SessionState(APPROVED.toString), Upcoming(new Date().getTime)))

          } yield (sessions, count)
          result.map { response =>
            response.asRight
          }
      }
    else
      Future.successful(NewSessionServiceError.AccessDenied.asLeft)

  def getPastSessions(
    pageNumber: Int,
    pageSize: Int,
    email: Option[String] = None
  ): Future[Either[NewSessionServiceError, (Seq[WithId[NewSession]], Int)]] =
    email match {
      case Some(key) =>
        val emailAndTopicSearch = ".*" + key.replaceAll("\\s", " ").toLowerCase + ".*"
        val result = for {
          sessions <- newSessionDao.list(
            And(
              DateTime(new Date().getTime),
              Or(
                Or(
                  MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                  MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                ),
                TopicMatch(emailAndTopicSearch)
              )
            ),
            pageNumber,
            pageSize,
            Some(SortBy((DateMatch, Descending)))
          )
          count <- newSessionDao.count(
            And(
              DateTime(new Date().getTime),
              Or(
                Or(
                  MatchPresenter(PresenterDetails("", emailAndTopicSearch)),
                  MatchCoPresenter(PresenterDetails("", emailAndTopicSearch))
                ),
                TopicMatch(emailAndTopicSearch)
              )
            )
          )
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
      case None =>
        val result = for {
          sessions <- newSessionDao.list(
            DateTime(new Date().getTime),
            pageNumber,
            pageSize,
            Some(SortBy(fields = (DateMatch, Descending)))
          )
          count <- newSessionDao.count(DateTime(new Date().getTime))
        } yield (sessions, count)
        result.map { response =>
          response.asRight
        }
    }

  def getUserSessions(pageNumber: Int, pageSize: Int, filter: String)(implicit
    userDetails: NewUserInformation
  ): Future[Either[NewSessionServiceError, (Seq[WithId[NewSession]], Int)]] =
    if (filter == "upcoming") {
      val presenter = PresenterDetails(userDetails.name, userDetails.email)
      val response = for {
        sessions <- newSessionDao.list(
          And(
            Or(Presenter(presenter), CoPresenter(presenter)),
            And(SessionsExcept("Cancelled"), Upcoming(new Date().getTime))
          ),
          pageNumber,
          pageSize
        )
        count <- newSessionDao.count(
          And(
            Or(Presenter(presenter), CoPresenter(presenter)),
            And(SessionsExcept("Cancelled"), Upcoming(new Date().getTime))
          )
        )
      } yield (sessions, count)
      response.map { sessions =>
        sessions.asRight
      }
    } else if (filter == "past") {
      val presenter = PresenterDetails(userDetails.name, userDetails.email)
      val response = for {
        sessions <- newSessionDao.list(
          And(
            Or(Presenter(presenter), CoPresenter(presenter)),
            DateTime(new Date().getTime)
          ),
          pageNumber,
          pageSize
        )
        count <- newSessionDao.count(
          And(
            Or(Presenter(presenter), CoPresenter(presenter)),
            DateTime(new Date().getTime)
          )
        )
      } yield (sessions, count)
      response.map { sessions =>
        sessions.asRight
      }
    } else
      Future.successful(NewSessionServiceError.SessionNotFound.asLeft)

  def bookSession(
    request: BookSessionUserRequest
  )(implicit userDetails: NewUserInformation): Future[Either[NewSessionServiceError, WithId[NewSession]]] =
    validateBookSessionRequest(request) match {
      case Invalid(error) => Future.successful(error.asLeft)
      case Valid(sessionRequest) =>
        val presenter = PresenterDetails(userDetails.name, userDetails.email)
        slotDao.get(sessionRequest.slotId).flatMap {
          case Some(slotWithId) =>
            if (slotWithId.entity.bookable) {
              val newSession = NewSession(
                presenter,
                sessionRequest.coPresenterDetail,
                slotWithId.entity.dateTime,
                slotWithId.entity.slotDuration,
                sessionRequest.topic,
                sessionRequest.category,
                sessionRequest.subCategory,
                sessionRequest.feedbackFormId,
                calculateSessionExpirationDate(new Date(slotWithId.entity.dateTime)),
                sessionRequest.sessionType,
                "PendingForAdmin",
                sessionRequest.sessionDescription,
                None,
                None,
                None,
                None,
                None,
                List(),
                None
              )
              newSessionDao.create(newSession).flatMap { sessionId =>
                val updatedSlot = slotWithId.entity.copy(sessionId = Some(sessionId), bookable = false)
                slotDao.update(slotWithId.id, _ => updatedSlot).map { _ =>
                  WithId(newSession, sessionId).asRight
                }
              }
            } else
              Future.successful(NewSessionServiceError.SlotNotAvailable.asLeft)
          case None => Future.successful(NewSessionServiceError.InvalidSlotId(sessionRequest.slotId).asLeft)
        }
    }

  private def calculateSessionExpirationDate(date: Date): Long = {
    val scheduledDate = dateTimeUtility.toLocalDateTimeEndOfDay(date)
    val expirationDate = scheduledDate.plusDays(2)

    dateTimeUtility.toMillis(expirationDate)
  }

  def updateNewSessions(
    id: String,
    coPresenterDetails: Option[PresenterDetails],
    dateTime: Option[Long] = None,
    topic: Option[String] = None,
    category: Option[String] = None,
    subCategory: Option[String] = None,
    feedbackFormId: Option[String] = None,
    sessionType: Option[String] = None,
    sessionDescription: Option[String] = None
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[NewSessionServiceError, Done]] =
    if (authorityUser.keycloakRole == Admin)
      newSessionDao.get(id).flatMap {
        case None => Future.successful(NewSessionServiceError.SessionNotFound.asLeft)
        case Some(session) =>
          val dateLong = session.entity.dateTime
          val date = new Date(dateLong)
          val feedbackExpriationDate: Long = calculateSessionExpirationDate(date)
          val updatedTopic = topic.fold(session.entity.topic)(identity)
          val updateddateTime = dateTime.fold(session.entity.dateTime)(identity)
          val updatecoPresenterDetails =
            coPresenterDetails.fold(session.entity.coPresenterDetails)(coPresenterDetail => Some(coPresenterDetail))
          val updatCategory = category.fold(session.entity.category)(identity)
          val updatSubCategory = subCategory.fold(session.entity.subCategory)(identity)
          val updateFeedbackFormId =
            feedbackFormId.fold(session.entity.feedbackFormId)(feedbackFormId => Some(feedbackFormId))
          val updateSessionType = sessionType.fold(session.entity.sessionType)(identity)
          val updateSessionDescription = sessionDescription.fold(session.entity.sessionDescription)(identity)

          val updatedSession = NewSession(
            session.entity.presenterDetails,
            coPresenterDetails = updatecoPresenterDetails,
            dateTime = updateddateTime,
            session.entity.sessionDuration,
            topic = updatedTopic,
            category = updatCategory,
            subCategory = updatSubCategory,
            feedbackFormId = updateFeedbackFormId,
            feedbackExpriationDate,
            sessionType = updateSessionType,
            session.entity.sessionState,
            sessionDescription = updateSessionDescription,
            session.entity.youtubeURL,
            session.entity.slideShareURL,
            session.entity.slideURL,
            session.entity.slidesApprovedBy,
            session.entity.sessionApprovedBy,
            session.entity.sessionTag,
            session.entity.remarks
          )
          newSessionDao.update(id, _ => updatedSession).map {
            case Some(_) => Done.asRight
            case None => NewSessionServiceError.InternalServerError.asLeft
          }
      }
    else
      Future.successful(NewSessionServiceError.AccessDenied.asLeft)

  def isSlideUrlValid(slideURL: Option[String]): Boolean =
    if (slideURL.isDefined)
      Pattern
        .compile("^[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$")
        .matcher(slideURL.get)
        .find()
    else
      true

  def updateSessionRequestValidation(
    sessionTopic: Option[String],
    sessionDescription: Option[String],
    slideURL: Option[String]
  ): Boolean =
    if (sessionTopic.isDefined || sessionDescription.isDefined || slideURL.isDefined) {
      val isEmpty = sessionTopic.getOrElse(" ").isEmpty || sessionDescription.getOrElse(" ").isEmpty || slideURL
            .getOrElse(" ")
            .isEmpty
      !isEmpty && isSlideUrlValid(slideURL) && sessionTopic.fold(true)(topic => topic.length <= 100)
    } else
      true

  def updateUserSession(
    id: String,
    topic: Option[String] = None,
    sessionDescription: Option[String] = None,
    slideURL: Option[String] = None,
    sessionTags: Option[List[String]] = None
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[NewSessionServiceError, Done]] =
    newSessionDao.get(id).flatMap {
      case Some(session) =>
        if (
          (authorityUser.email == session.entity.presenterDetails.email || authorityUser.email == session.entity.coPresenterDetails.get.email) &&
          (!session.entity.sessionState.equals(sessionState.APPROVED.toString) && !session.entity.sessionState
            .equals(sessionState.COMPLETED.toString))
        )
          if (updateSessionRequestValidation(topic, sessionDescription, slideURL)) {
            val updatedTopic = topic.fold(session.entity.topic)(identity)
            val updateUserSessionDescription = sessionDescription.fold(session.entity.sessionDescription)(identity)
            val updateSessionTag = sessionTags.fold(session.entity.sessionTag)(identity)
            val updatedSlideUrl = slideURL.fold(session.entity.slideURL) { url =>
              Some(url)
            }
            val updateUserSession = session.entity.copy(
              topic = updatedTopic,
              sessionDescription = updateUserSessionDescription,
              slideURL = updatedSlideUrl,
              sessionTag = updateSessionTag
            )

            newSessionDao.update(id, _ => updateUserSession).map {
              case Some(_) =>
                Done.asRight
              case None => NewSessionServiceError.InternalServerError.asLeft
            }
          } else
            Future.successful(NewSessionServiceError.InvalidRequest.asLeft)
        else
          Future.successful(NewSessionServiceError.AccessDenied.asLeft)

      case None => Future.successful(NewSessionServiceError.SessionNotFound.asLeft)
    }

  def validateBookSessionRequest(
    sessionRequest: BookSessionUserRequest
  ): Validated[NewSessionServiceError, BookSessionUserRequest] =
    if (
      sessionRequest.topic.isEmpty || sessionRequest.sessionDescription.isEmpty || sessionRequest.sessionType.isEmpty || sessionRequest.category.isEmpty || sessionRequest.subCategory.isEmpty
    )
      NewSessionServiceError.MandatoryFieldsNotFound.invalid
    else
      sessionRequest.valid

}

object NewSessionService {

  sealed trait NewSessionServiceError

  object NewSessionServiceError {

    case object AccessDenied extends NewSessionServiceError

    case object SlotNotAvailable extends NewSessionServiceError

    case class InvalidSlotId(id: String) extends NewSessionServiceError

    case object SessionNotFound extends NewSessionServiceError

    case object FilterNotFound extends NewSessionServiceError

    case object InternalServerError extends NewSessionServiceError

    case object NotFound extends NewSessionServiceError

    case object MandatoryFieldsNotFound extends NewSessionServiceError

    case object InvalidRequest extends NewSessionServiceError

  }
}
