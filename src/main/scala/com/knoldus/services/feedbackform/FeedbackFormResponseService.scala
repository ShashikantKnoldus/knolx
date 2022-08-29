package com.knoldus.services.feedbackform

import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao.{ FeedbackResponseScore, StoredSessionId, StoredUserId }
import com.knoldus.dao.feedbackform.{ FeedbackFormDao, FeedbackFormsResponseDao }
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.{ Active, ExpirationDate, PresenterSession, SessionDate }
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.CheckBanUser
import com.knoldus.domain.feedbackform.{ FeedbackForm, Question, UserFeedbackResponse, UserSessionFeedbackResponse }
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.UserInformation
import com.knoldus.routes.contract.feedbackform._
import com.knoldus.routes.contract.user.UserResponse
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.feedbackform.FeedbackFormResponseService.FeedbackFormResponseServiceError
import com.typesafe.config.Config

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class FeedbackFormResponseService(
  conf: Config,
  mailerService: MailerService,
  feedbackFormResponseDao: FeedbackFormsResponseDao,
  feedbackFormDao: FeedbackFormDao,
  sessionDao: SessionDao,
  userDao: UserDao
)(
  implicit val ec: ExecutionContext,
  implicit val logger: LoggingAdapter
) {

  def getFeedbackFormForToday(
    userId: String
  ): Future[Either[FeedbackFormResponseServiceError, List[(FeedbackSession, WithId[FeedbackForm])]]] = {
    def getBanUserIds(userData: Seq[WithId[UserInformation]]): Future[Seq[String]] =
      Future.successful(userData.map { user =>
        user.id
      })

    def checkIfUserIdContain(
      ids: Seq[String]
    ): Future[Either[FeedbackFormResponseServiceError, List[(FeedbackSession, WithId[FeedbackForm])]]] =
      if (ids.contains(userId))
        Future.successful(FeedbackFormResponseServiceError.FeedbackError.asLeft)
      else {
        val sessionInfo = sessionDao.listAll(
          And(And(Active(true), PresenterSession(userId)), And(ExpirationDate(new Date()), SessionDate(new Date())))
        )

        val feedbackSessionsInfoList = sessionInfo.map { activeSessions =>
          activeSessions.map { session =>
            feedbackFormResponseDao.get(And(StoredSessionId(session.id), StoredUserId(userId))) map {
              case Some(userFeedbackResponse: WithId[UserFeedbackResponse]) =>
                logger.info(userFeedbackResponse.id)
                FeedbackSession(
                  userId,
                  session.entity.email,
                  session.entity.date,
                  session.entity.session,
                  session.entity.feedbackFormId,
                  session.entity.topic,
                  session.entity.meetup,
                  session.entity.rating,
                  session.entity.cancelled,
                  session.entity.active,
                  session.id,
                  session.entity.expirationDate,
                  feedbackSubmitted = true
                )
              case _ =>
                FeedbackSession(
                  userId,
                  session.entity.email,
                  session.entity.date,
                  session.entity.session,
                  session.entity.feedbackFormId,
                  session.entity.topic,
                  session.entity.meetup,
                  session.entity.rating,
                  session.entity.cancelled,
                  session.entity.active,
                  session.id,
                  session.entity.expirationDate
                )
            }
          }
        }

        def getSessionFeedbackInfo(
          feedbackSessionList: Seq[FeedbackSession]
        ): Future[Seq[(FeedbackSession, WithId[FeedbackForm])]] =
          for {
            optSessionList <- Future.sequence(
              feedbackSessionList.map { feedbackSession =>
                feedbackFormDao.get(feedbackSession.feedbackFormId).map(feedbackForm => (feedbackSession, feedbackForm))
              }
            )
          } yield optSessionList.map {
            case (feedbackSession, feedbackForm) =>
              feedbackForm match {
                case Some(form) =>
                  (feedbackSession, form)

                case None =>
                  throw new RuntimeException("Feedback Session not found")
              }
          }

        val sessionFormList = for {
          feedbackSessionList <- feedbackSessionsInfoList
          feedbackSessions <- Future.sequence(feedbackSessionList)
          sessionFeedbackInfoList <- getSessionFeedbackInfo(feedbackSessions)
        } yield sessionFeedbackInfoList.toList

        sessionFormList.map { listFeedbackSessionWithForm =>
          listFeedbackSessionWithForm.asRight
        }

      }

    for {
      userInfo <- userDao.listAll(CheckBanUser(new Date()))
      feedbackSessionInfo <- getBanUserIds(userInfo)
      feedbackSessionWithFeedbackForm <- checkIfUserIdContain(feedbackSessionInfo)
    } yield feedbackSessionWithFeedbackForm

  }

  def storeFeedbackResponse(
    userId: String,
    sessionId: String,
    feedbackFormId: String,
    responses: List[String],
    score: Double
  ): Future[Either[FeedbackFormResponseServiceError, UserFeedbackResponse]] = {
    val feedbackFormResponse = FeedbackResponse(sessionId, feedbackFormId, responses, score)
    val sanitizedResponse: Future[Option[(ResponseHeader, List[QuestionResponse])]] = deepValidatedFeedbackResponses(
      feedbackFormResponse
    )
    val userEntity = userDao.get(userId).map(_.get)
    val userInfo = userEntity.map { userData =>
      val lastBannedOn = userData.entity.lastBannedOn
      UserInformation(
        userData.entity.email,
        userData.entity.active,
        userData.entity.admin,
        userData.entity.coreMember,
        userData.entity.superUser,
        userData.entity.banTill,
        userData.entity.banCount,
        lastBannedOn,
        userData.entity.nonParticipating,
        userData.entity.department
      )
    }

    val storeFeedbacks = sanitizedResponse.map { sanitized =>
      val (header, response) = sanitized.get
      userInfo.map { userinfo =>
        val feedbackResponseData = UserSessionFeedbackResponse(
          userinfo.email,
          userinfo.coreMember,
          header.email,
          userId,
          feedbackFormResponse.sessionId,
          header.topic,
          meetup = header.meetUp,
          header.date,
          header.session,
          response,
          new Date(),
          feedbackFormResponse.score
        )

        UserFeedbackResponse(
          feedbackResponseData.sessionId,
          feedbackResponseData.userId,
          feedbackResponseData.coreMember,
          feedbackResponseData.email,
          feedbackResponseData.feedbackResponse,
          feedbackResponseData.meetup,
          feedbackResponseData.presenter,
          feedbackResponseData.responseDate,
          feedbackResponseData.score,
          feedbackResponseData.session,
          feedbackResponseData.sessionTopic,
          feedbackResponseData.sessiondate
        )
      }
    }

    storeFeedbacks.flatMap { feedbackData =>
      feedbackData.map { userfeedback =>
        val responseData =
          feedbackFormResponseDao.get(And(StoredSessionId(sessionId), StoredUserId(userId)))
        responseData.map {
          case Some(userFeedbackResponse: WithId[UserFeedbackResponse]) =>
            logger.info(userFeedbackResponse.id)
            val updateResult: Future[Option[WithId[UserFeedbackResponse]]] =
              feedbackFormResponseDao.update(userFeedbackResponse.id, _ => userfeedback)
            updateResult.onComplete {
              case Success(feedbackResponse: Option[WithId[UserFeedbackResponse]]) =>
                logger.info(feedbackResponse.get.id)
                sendEmailToUser(userfeedback.sessionId, userfeedback.email)

              case Failure(_) => throw new RuntimeException("Something went Wrong")
            }

          case None =>
            val createResult = feedbackFormResponseDao.create(userfeedback)
            createResult.onComplete {
              case Success(responseId: String) =>
                logger.info(responseId)
                sendEmailToUser(userfeedback.sessionId, userfeedback.email)

                val sessionInfo =
                  feedbackFormResponseDao.listAll(And(StoredSessionId(sessionId), FeedbackResponseScore(0.0)))
                sessionInfo.map { session =>
                  session.foreach { sessionScore =>
                    updateRatingIfCoreMember(userId, sessionId, List(sessionScore.entity.score))
                  }
                }
              case Failure(_) => throw new RuntimeException("Something went Wrong")
            }
        }
        userfeedback.asRight
      }
    }
  }

  def updateRatingIfCoreMember(userId: String, sessionId: String, scores: List[Double]): Unit = {
    val user = userDao.get(userId).map(_.get)
    user.map { userInfo =>
      if (userInfo.entity.coreMember) {
        val sessionInfo = sessionDao.get(sessionId).map(_.get)

        val scoresWithoutZero = scores.filterNot(_ == 0)
        val sessionScore = if (scoresWithoutZero.nonEmpty) scoresWithoutZero.sum / scoresWithoutZero.length else 0.00

        val updatedRating = sessionScore match {
          case good if sessionScore >= 60.00 =>
            logger.info(good.toString)
            "Good"

          case average if sessionScore >= 30.00 =>
            logger.info(average.toString)
            "Average"

          case _ => "Bad"
        }

        sessionInfo.map { data =>
          val youtubeUrl = data.entity.youtubeURL
          val session = Session(
            data.entity.userId,
            data.entity.email,
            data.entity.date,
            data.entity.session,
            data.entity.category,
            data.entity.subCategory,
            data.entity.feedbackFormId,
            data.entity.topic,
            data.entity.feedbackExpirationDays,
            data.entity.meetup,
            data.entity.brief,
            updatedRating,
            sessionScore,
            data.entity.cancelled,
            data.entity.active,
            data.entity.expirationDate,
            youtubeUrl,
            data.entity.slideShareURL,
            data.entity.temporaryYoutubeURL,
            data.entity.reminder,
            data.entity.notification
          )
          sessionDao.update(data.id, _ => session)
        }
      }
    }
  }

  def sendEmailToUser(sessionId: String, userMail: String): Future[Either[FeedbackFormResponseServiceError, String]] = {
    val sessionInfo = sessionDao.get(sessionId).map(_.get)
    sessionInfo.map { session =>
      val content = EmailContent.setContentForFeedback(session.entity.email, session.entity.topic)
      mailerService.sendMessage(
        userMail,
        "Feedback Successfully Registered!",
        content
      )
    }
    Future.successful("".asRight)
  }

  def deepValidatedFeedbackResponses(
    userResponse: FeedbackResponse
  ): Future[Option[(ResponseHeader, List[QuestionResponse])]] =
    sessionDao.get(userResponse.sessionId).flatMap { session =>
      session.fold {
        val badResponse: Option[(ResponseHeader, List[QuestionResponse])] = None
        Future.successful(badResponse)
      } { session =>
        feedbackFormDao.get(userResponse.feedbackFormId).map {
          case Some(feedbackForm) =>
            val questions = feedbackForm.entity.questions
            if (questions.size == userResponse.responses.size) {
              val sanitizedResponses = sanitizeResponses(questions, userResponse.responses).toList.flatten
              if (questions.size == sanitizedResponses.size)
                Some(
                  (
                    ResponseHeader(
                      session.entity.topic,
                      session.entity.email,
                      session.entity.date,
                      session.entity.session,
                      session.entity.meetup
                    ),
                    sanitizedResponses
                  )
                )
              else
                None
            } else
              None
          case None => None
        }
      }
    }

  def sanitizeResponses(
    questions: Seq[Question],
    responses: List[String]
  ): Seq[Option[QuestionResponse]] =
    for ((question, response) <- questions zip responses) yield (question.questionType, question.mandatory) match {
      case ("MCQ", true) =>
        if (question.options.contains(response) && response.nonEmpty)
          Some(
            QuestionResponse(question.question, question.options, response)
          )
        else
          None
      case ("COMMENT", true) =>
        if (response.nonEmpty)
          Some(
            QuestionResponse(question.question, question.options, response)
          )
        else
          None
      case ("MCQ", false) => None
      case ("COMMENT", false) =>
        Some(QuestionResponse(question.question, question.options, response))
      case _ => None

    }

  def getBanUserDetails(userId: String): Future[Either[FeedbackFormResponseServiceError, UserResponse]] = {
    def getBanUserIds(userData: Seq[WithId[UserInformation]]): Future[Seq[String]] =
      Future.successful(userData.map { user =>
        user.id
      })

    def checkIfIdcontains(ids: Seq[String]): Future[Either[FeedbackFormResponseServiceError, UserResponse]] =
      if (ids.contains(userId)) {
        val user = userDao.get(userId).map(_.get)
        val userResponseData = user.map { userdata =>
          UserResponse(
            userdata.entity.email,
            userdata.entity.active,
            userdata.entity.admin,
            userdata.entity.coreMember,
            userdata.entity.superUser,
            userdata.entity.banTill,
            userdata.entity.banCount,
            userdata.entity.department
          )
        }
        userResponseData.map { userResponse =>
          userResponse.asRight
        }

      } else
        Future.successful(FeedbackFormResponseServiceError.FeedbackNotFoundError.asLeft)

    for {
      userInfo <- userDao.listAll(CheckBanUser(new Date()))
      banUserIds <- getBanUserIds(userInfo)
      banUserInfo <- checkIfIdcontains(banUserIds)
    } yield banUserInfo

  }

  def fetchFeedbackResponse(
    sessionId: String,
    userId: String
  ): Future[Either[FeedbackFormResponseServiceError, WithId[UserFeedbackResponse]]] = {
    val storedResponse: Future[Option[WithId[UserFeedbackResponse]]] =
      feedbackFormResponseDao.get(And(StoredSessionId(sessionId), StoredUserId(userId)))
    storedResponse.map {
      case Some(storedData: WithId[UserFeedbackResponse]) =>
        storedData.asRight
      case _ => FeedbackFormResponseServiceError.FeedbackError.asLeft
    }
  }

  def getAllResponseEmailsPerSession(
    sessionId: String
  ): Future[Either[FeedbackFormResponseServiceError, List[String]]] =
    feedbackFormResponseDao.listAll(StoredSessionId(sessionId)).map { feedbackFormResponses =>
      if (feedbackFormResponses.nonEmpty)
        feedbackFormResponses
          .map { feedback =>
            feedback.entity.email
          }
          .toList
          .asRight
      else
        FeedbackFormResponseServiceError.FeedbackNotFoundError.asLeft
    }
}

object FeedbackFormResponseService {

  sealed trait FeedbackFormResponseServiceError

  object FeedbackFormResponseServiceError {

    case object FeedbackError extends FeedbackFormResponseServiceError

    case object FeedbackAccessDeniedError extends FeedbackFormResponseServiceError

    case object FeedbackNotFoundError extends FeedbackFormResponseServiceError

  }

}
