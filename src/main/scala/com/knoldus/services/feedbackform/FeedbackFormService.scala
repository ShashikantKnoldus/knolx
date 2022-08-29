package com.knoldus.services.feedbackform

import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormDao
import com.knoldus.dao.feedbackform.FeedbackFormDao.ActiveFeedbackForms
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.{ Active, ExpirationDate }
import com.knoldus.domain.feedbackform.{ FeedbackForm, FeedbackFormList, Question }
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.feedbackform.FeedbackFormData
import com.knoldus.services.feedbackform.FeedbackFormService.{ FeedbackFormServiceError, FeedbackServiceError }

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class FeedbackFormService(feedbackFormDao: FeedbackFormDao, sessionDao: SessionDao)(
  implicit val ec: ExecutionContext,
  implicit val logger: LoggingAdapter
) {

  def createFeedbackForm(
    name: String,
    questions: List[Question]
  )(implicit authorityUser: NewUserInformation): Future[Either[FeedbackFormServiceError, WithId[FeedbackForm]]] =
    if (authorityUser.keycloakRole == Admin) {
      val feedbackFormEntity = FeedbackForm(
        name = name,
        questions = questions,
        active = true
      )
      feedbackFormDao.create(feedbackFormEntity).map { id =>
        val feedbackInfo = WithId(feedbackFormEntity, id)
        feedbackInfo.asRight
      }
    } else
      Future.successful(FeedbackServiceError.AccessDenied.asLeft)

  def delete(id: String)(implicit authorityUser: NewUserInformation): Future[Either[FeedbackFormServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin) {
      val feedback = feedbackFormDao.get(id)
      feedback.map {
        case Some(form) =>
          val feedbackForm = FeedbackForm(
            name = form.entity.name,
            questions = form.entity.questions,
            active = false
          )
          feedbackFormDao.update(id, _ => feedbackForm)
          ().asRight

        case None =>
          FeedbackServiceError.FeedbackNotFoundError.asLeft
      }
    } else
      Future.successful(FeedbackServiceError.AccessDenied.asLeft)

  def getAllFeedbackForm(implicit
    authorityUser: NewUserInformation
  ): Future[Either[FeedbackFormServiceError, List[FeedbackFormList]]] =
    if (authorityUser.keycloakRole == Admin) {
      val feedback = feedbackFormDao.listAll(ActiveFeedbackForms(true))
      val feedbackForm = feedback.map { form =>
        val allForm = form.map { forms =>
          FeedbackFormList(forms.entity.name, forms.entity.questions, forms.entity.active, forms.id)
        }
        allForm
      }
      feedbackForm.map { feedbackFormList =>
        feedbackFormList.toList.asRight
      }
    } else
      Future.successful(FeedbackServiceError.AccessDenied.asLeft)

  def getFeedbackFormById(
    id: String
  )(implicit authorityUser: NewUserInformation): Future[Either[FeedbackFormServiceError, FeedbackFormData]] = {
    def getActiveSessionsFeedbackId(sessions: Seq[WithId[Session]]): Future[Seq[String]] =
      Future.successful(sessions.map { session =>
        session.entity.feedbackFormId
      })

    def checkIfFeedbackContain(ids: Seq[String]): Future[Either[FeedbackFormServiceError, FeedbackFormData]] =
      if (ids.contains(id)) {
        val feedback = feedbackFormDao.get(id).map(_.get)
        val feedbackForm = feedback.map { form =>
          FeedbackFormData(form.entity.name, form.entity.questions, form.entity.active, id)
        }
        feedbackForm.map(feedbackData => feedbackData.asRight)
      } else
        Future.successful(FeedbackServiceError.FeedbackNotFoundError.asLeft)

    if (authorityUser.keycloakRole == Admin) {
      val allFeedbackId = for {
        allSession <- sessionDao.listAll(And(Active(true), ExpirationDate(new Date())))
        allFeedBackId <- getActiveSessionsFeedbackId(allSession)
        feedbackFormInfo <- checkIfFeedbackContain(allFeedBackId)
      } yield feedbackFormInfo
      allFeedbackId
    } else
      Future.successful(FeedbackServiceError.AccessDenied.asLeft)
  }

  def update(
    id: String,
    name: Option[String] = None,
    questions: Option[List[Question]] = None
  )(implicit authorityUser: NewUserInformation): Future[Either[FeedbackFormServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin)
      feedbackFormDao.get(id).map {
        case Some(form) =>
          val feedback = FeedbackForm(
            name = name.getOrElse(form.entity.name),
            questions = questions.getOrElse(form.entity.questions),
            active = true
          )
          feedbackFormDao.update(id, _ => feedback)
          ().asRight
        case None => FeedbackServiceError.FeedbackNotFoundError.asLeft
      }
    else
      Future.successful(FeedbackServiceError.AccessDenied.asLeft)

  def listAllFeedbackForms(
    pageNumber: Int
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[FeedbackFormServiceError, (Seq[WithId[FeedbackForm]], Int)]] =
    if (authorityUser.keycloakRole == Admin) {
      val feedbackFormList = for {
        feedbackForm <- feedbackFormDao.list(ActiveFeedbackForms(true), pageNumber, 10)
        count <- feedbackFormDao.count(ActiveFeedbackForms(true))
      } yield (feedbackForm, count)

      feedbackFormList.map { feedbackFormEntityWithId =>
        feedbackFormEntityWithId.asRight
      }
    } else
      Future.successful(FeedbackServiceError.AccessDenied.asLeft)

}

object FeedbackFormService {

  sealed trait FeedbackFormServiceError

  object FeedbackServiceError {

    case object FeedbackError extends FeedbackFormServiceError

    case object AccessDenied extends FeedbackFormServiceError

    case object FeedbackNotFoundError extends FeedbackFormServiceError

  }

}
