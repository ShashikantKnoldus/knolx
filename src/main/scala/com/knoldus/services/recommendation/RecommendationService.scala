package com.knoldus.services.recommendation

import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, IdIs, Or }
import com.knoldus.dao.recommendation.RecommendationDao._
import com.knoldus.dao.recommendation.RecommendationsResponseDao.{ Email, RecommendationId }
import com.knoldus.dao.recommendation.{ RecommendationDao, RecommendationsResponseDao }
import com.knoldus.dao.sorting.Direction.Descending
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{ Admin, SuperUser }
import com.knoldus.domain.recommendation.{ Recommendation, RecommendationsResponse }
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.recommendation.{
  AddRecommendationResponse,
  RecommendationInformation,
  RecommendationResponse
}
import com.knoldus.services.email.{ EmailContent, MailerService }
import com.knoldus.services.recommendation.RecommendationService.RecommendationServiceError

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure

class RecommendationService(
  recommendationDao: RecommendationDao,
  recommendationsResponseDao: RecommendationsResponseDao,
  userDao: UserDao,
  mailerService: MailerService
)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {

  def addRecommendation(
    request: RecommendationInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] = {
    val recommendation = Recommendation(
      email = request.email,
      name = request.name,
      topic = request.topic,
      description = request.description,
      submissionDate = new Date(),
      updateDate = new Date(),
      approved = false,
      decline = false,
      pending = true,
      done = false,
      book = false,
      upVote = 0,
      downVote = 0
    )
    recommendationDao.create(recommendation).map { id =>
      if (id.nonEmpty) {
        val recommendationInfo = WithId(recommendation, id)
        sendRecommendationMail(recommendation)
        recommendationInfo.asRight
      } else
        RecommendationServiceError.RecommendationError.asLeft
    }
  }

  def sendRecommendationMail(recommendationInformation: Recommendation): Future[akka.Done] = {
    val receiver = getAllAdminAndSuperUser
    receiver.flatMap { receiverEmails =>
      mailerService
        .sendMessage(
          receiverEmails,
          "Knolx/Meetup Recommendation",
          EmailContent.setRecommendationEmailContent(recommendationInformation)
        )
        .andThen {
          case Failure(exception) => logger.error(exception.toString)
        }
        .recover {
          case _ => akka.Done
        }
    }
  }

  def getAllAdminAndSuperUser: Future[List[String]] =
    userDao.listAll(Or(Admin(true), SuperUser(true))).map { listOfUsers =>
      listOfUsers.map { user =>
        user.entity.email
      }.toList
    }

  def getRecommendationById(
    recommendationId: String
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] =
    recommendationDao.get(recommendationId).flatMap { recommendation =>
      if (recommendation.isEmpty)
        Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      else {
        val recommendationInfo = recommendation.get
        Future.successful(recommendationInfo.asRight)
      }
    }

  def approveRecommendation(id: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] = {
    val recommendation = recommendationDao.get(id)
    import com.knoldus.domain.user.KeycloakRole.Admin
    if (authorityUser.keycloakRole == Admin)
      recommendation.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = true,
            decline = false,
            pending = true,
            done = recommendationWithId.entity.done,
            book = recommendationWithId.entity.book,
            upVote = recommendationWithId.entity.upVote,
            downVote = recommendationWithId.entity.downVote
          )
          recommendationDao.update(id, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    else
      Future.successful(RecommendationServiceError.RecommendationAccessDeniedError.asLeft)
  }

  def declineRecommendation(id: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] = {
    val recommendation = recommendationDao.get(id)
    import com.knoldus.domain.user.KeycloakRole.Admin
    if (authorityUser.keycloakRole == Admin)
      recommendation.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = false,
            decline = true,
            pending = false,
            done = false,
            book = false,
            upVote = recommendationWithId.entity.upVote,
            downVote = recommendationWithId.entity.downVote
          )
          recommendationDao.update(id, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    else
      Future.successful(RecommendationServiceError.RecommendationAccessDeniedError.asLeft)
  }

  def pendingRecommendation(id: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] = {

    val recommendation = recommendationDao.get(id)
    import com.knoldus.domain.user.KeycloakRole.Admin
    if (authorityUser.keycloakRole == Admin)
      recommendation.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = recommendationWithId.entity.approved,
            decline = recommendationWithId.entity.decline,
            pending = true,
            done = false,
            book = false,
            upVote = recommendationWithId.entity.upVote,
            downVote = recommendationWithId.entity.downVote
          )
          recommendationDao.update(id, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    else
      Future.successful(RecommendationServiceError.RecommendationAccessDeniedError.asLeft)
  }

  def doneRecommendation(id: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] = {

    val recommendation = recommendationDao.get(id)
    import com.knoldus.domain.user.KeycloakRole.Admin
    if (authorityUser.keycloakRole == Admin)
      recommendation.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = recommendationWithId.entity.approved,
            decline = recommendationWithId.entity.decline,
            pending = false,
            done = true,
            book = recommendationWithId.entity.book,
            upVote = recommendationWithId.entity.upVote,
            downVote = recommendationWithId.entity.downVote
          )
          recommendationDao.update(id, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    else
      Future.successful(RecommendationServiceError.RecommendationAccessDeniedError.asLeft)
  }

  def allPendingRecommendation: Future[Either[RecommendationServiceError, Int]] = {
    val filter = And(Approved(false), Declined(false))
    val count = recommendationDao.count(filter)
    count.map { result =>
      if (result.isEmpty)
        RecommendationServiceError.RecommendationError.asLeft
      result.asRight
    }
  }

  def bookRecommendation(id: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] = {
    val filter = And(IdIs(id), Approved(true))
    val recommendation = recommendationDao.get(filter)
    recommendation.flatMap {
      case Some(recommendationWithId) =>
        val recommendation = Recommendation(
          email = recommendationWithId.entity.email,
          name = recommendationWithId.entity.name,
          topic = recommendationWithId.entity.topic,
          description = recommendationWithId.entity.description,
          submissionDate = recommendationWithId.entity.submissionDate,
          updateDate = new Date,
          approved = recommendationWithId.entity.approved,
          decline = recommendationWithId.entity.decline,
          pending = false,
          done = recommendationWithId.entity.done,
          book = true,
          upVote = recommendationWithId.entity.upVote,
          downVote = recommendationWithId.entity.downVote
        )
        recommendationDao.update(id, _ => recommendation).map {
          case Some(result) => result.asRight
          case None => RecommendationServiceError.RecommendationError.asLeft
        }
      case None =>
        Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
    }
  }

  def cancelBookedRecommendation(id: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] = {
    val filter = And(IdIs(id), Approved(true))
    val recommendation = recommendationDao.get(filter)
    import com.knoldus.domain.user.KeycloakRole.Admin
    if (authorityUser.keycloakRole == Admin)
      recommendation.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = recommendationWithId.entity.approved,
            decline = recommendationWithId.entity.decline,
            pending = true,
            done = false,
            book = false,
            upVote = recommendationWithId.entity.upVote,
            downVote = recommendationWithId.entity.downVote
          )
          recommendationDao.update(id, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    else
      Future.successful(RecommendationServiceError.RecommendationAccessDeniedError.asLeft)
  }

  def listRecommendation(
    filter: String
  ): Future[Either[RecommendationServiceError, List[AddRecommendationResponse]]] =
    filter match {
      case "all" =>
        getAllRecommendation.map { recommendations =>
          recommendations.flatMap { res =>
            res.asRight
          }
        }
      case "pending" =>
        getPendingRecommendation.map { recommendations =>
          recommendations.flatMap { recommendationInfo =>
            recommendationInfo.asRight
          }
        }
      case "approved" =>
        getApprovedRecommendation.map { recommendations =>
          recommendations.flatMap { recommendationInfo =>
            recommendationInfo.asRight
          }
        }
      case "decline" =>
        getDeclinedRecommendation.map { recommendations =>
          recommendations.flatMap { recommendationInfo =>
            recommendationInfo.asRight
          }
        }
      case "book" =>
        getBookedRecommendation.map { recommendations =>
          recommendations.flatMap { recommendationInfo =>
            recommendationInfo.asRight
          }
        }
      case "done" =>
        getDoneRecommendation.map { recommendations =>
          recommendations.flatMap { recommendationInfo =>
            recommendationInfo.asRight
          }
        }
    }

  def getAllRecommendation: Future[Either[RecommendationServiceError, List[AddRecommendationResponse]]] = {
    val recommendations: Future[Seq[WithId[Recommendation]]] =
      recommendationDao.listAll(Done(false), Some(SortBy((DateMatch, Descending))))
    val recommendationInfo = recommendations.map { recommendation =>
      val allRecommendation = recommendation.map { recommendations =>
        AddRecommendationResponse(
          recommendations.entity.email,
          recommendations.entity.name,
          recommendations.entity.topic,
          recommendations.entity.description,
          recommendations.entity.submissionDate,
          recommendations.entity.updateDate,
          recommendations.entity.approved,
          recommendations.entity.decline,
          recommendations.entity.pending,
          recommendations.entity.done,
          recommendations.entity.book,
          recommendations.entity.upVote,
          recommendations.entity.downVote,
          recommendations.id
        )
      }
      allRecommendation
    }
    recommendationInfo.map {
      _.toList.asRight
    }
  }

  def getPendingRecommendation: Future[Either[RecommendationServiceError, List[AddRecommendationResponse]]] = {
    val recommendations = recommendationDao.listAll(Pending(true), Some(SortBy((DateMatch, Descending))))
    val recommendationInfo = recommendations.map { recommendation =>
      val allPendingRecommendations = recommendation.map { recommendations =>
        AddRecommendationResponse(
          recommendations.entity.email,
          recommendations.entity.name,
          recommendations.entity.topic,
          recommendations.entity.description,
          recommendations.entity.submissionDate,
          recommendations.entity.updateDate,
          recommendations.entity.approved,
          recommendations.entity.decline,
          recommendations.entity.pending,
          recommendations.entity.done,
          recommendations.entity.book,
          recommendations.entity.upVote,
          recommendations.entity.downVote,
          recommendations.id
        )
      }
      allPendingRecommendations
    }
    recommendationInfo.map {
      _.toList.asRight
    }
  }

  def getApprovedRecommendation: Future[Either[RecommendationServiceError, List[AddRecommendationResponse]]] = {
    val recommendations =
      recommendationDao.listAll(And(Approved(true), Done(false)), Some(SortBy((DateMatch, Descending))))
    val recommendationInfo = recommendations.map { recommendation =>
      val allApprovedRecommendations = recommendation.map { recommendations =>
        AddRecommendationResponse(
          recommendations.entity.email,
          recommendations.entity.name,
          recommendations.entity.topic,
          recommendations.entity.description,
          recommendations.entity.submissionDate,
          recommendations.entity.updateDate,
          recommendations.entity.approved,
          recommendations.entity.decline,
          recommendations.entity.pending,
          recommendations.entity.done,
          recommendations.entity.book,
          recommendations.entity.upVote,
          recommendations.entity.downVote,
          recommendations.id
        )
      }
      allApprovedRecommendations
    }
    recommendationInfo.map {
      _.toList.asRight
    }
  }

  def getDeclinedRecommendation: Future[Either[RecommendationServiceError, List[AddRecommendationResponse]]] = {
    val recommendations =
      recommendationDao.listAll(And(Done(false), Declined(true)), Some(SortBy((DateMatch, Descending))))
    val recommendationInfo = recommendations.map { recommendation =>
      val allDeclinedRecommendation = recommendation.map { recommendations =>
        AddRecommendationResponse(
          recommendations.entity.email,
          recommendations.entity.name,
          recommendations.entity.topic,
          recommendations.entity.description,
          recommendations.entity.submissionDate,
          recommendations.entity.updateDate,
          recommendations.entity.approved,
          recommendations.entity.decline,
          recommendations.entity.pending,
          recommendations.entity.done,
          recommendations.entity.book,
          recommendations.entity.upVote,
          recommendations.entity.downVote,
          recommendations.id
        )
      }
      allDeclinedRecommendation
    }
    recommendationInfo.map {
      _.toList.asRight
    }
  }

  def getBookedRecommendation: Future[Either[RecommendationServiceError, List[AddRecommendationResponse]]] = {
    val recommendations = recommendationDao.listAll(And(Book(true), Done(false)), Some(SortBy((DateMatch, Descending))))
    val recommendationInfo = recommendations.map { recommendation =>
      val allBookedRecommendation = recommendation.map { recommendations =>
        AddRecommendationResponse(
          recommendations.entity.email,
          recommendations.entity.name,
          recommendations.entity.topic,
          recommendations.entity.description,
          recommendations.entity.submissionDate,
          recommendations.entity.updateDate,
          recommendations.entity.approved,
          recommendations.entity.decline,
          recommendations.entity.pending,
          recommendations.entity.done,
          recommendations.entity.book,
          recommendations.entity.upVote,
          recommendations.entity.downVote,
          recommendations.id
        )
      }
      allBookedRecommendation
    }
    recommendationInfo.map {
      _.toList.asRight
    }
  }

  def getDoneRecommendation: Future[Either[RecommendationServiceError, List[AddRecommendationResponse]]] = {
    val recommendations = recommendationDao.listAll(Done(true), Some(SortBy((DateMatch, Descending))))
    val recommendationInfo = recommendations.map { recommendation =>
      val allDoneRecommendation = recommendation.map { recommendations =>
        AddRecommendationResponse(
          recommendations.entity.email,
          recommendations.entity.name,
          recommendations.entity.topic,
          recommendations.entity.description,
          recommendations.entity.submissionDate,
          recommendations.entity.updateDate,
          recommendations.entity.approved,
          recommendations.entity.decline,
          recommendations.entity.pending,
          recommendations.entity.done,
          recommendations.entity.book,
          recommendations.entity.upVote,
          recommendations.entity.downVote,
          recommendations.id
        )
      }
      allDoneRecommendation
    }
    recommendationInfo.map {
      _.toList.asRight
    }
  }

  def getVote(recommendationId: String, email: String): Future[Either[RecommendationServiceError, String]] = {
    val filter = And(RecommendationId(recommendationId), Email(email))
    val data = recommendationsResponseDao.get(filter)
    data.map {
      case Some(value) =>
        (value.entity.upVote, value.entity.downVote) match {
          case (true, false) => "upvote".asRight
          case (false, true) => "downvote".asRight
          case _ => "".asRight
        }
      case None => "".asRight
    }
  }

  def upVote(
    recommendationId: String,
    alreadyVoted: Boolean
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] =
    if (alreadyVoted) {
      val recommendationInfo = recommendationDao.get(recommendationId)
      recommendationInfo.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = recommendationWithId.entity.approved,
            decline = recommendationWithId.entity.decline,
            pending = recommendationWithId.entity.pending,
            done = recommendationWithId.entity.done,
            book = recommendationWithId.entity.book,
            upVote = recommendationWithId.entity.upVote + 1,
            downVote = recommendationWithId.entity.downVote - 1
          )
          recommendationDao.update(recommendationId, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    } else {
      val recommendationInfo = recommendationDao.get(recommendationId)
      recommendationInfo.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = recommendationWithId.entity.approved,
            decline = recommendationWithId.entity.decline,
            pending = recommendationWithId.entity.pending,
            done = recommendationWithId.entity.done,
            book = recommendationWithId.entity.book,
            upVote = recommendationWithId.entity.upVote + 1,
            downVote = recommendationWithId.entity.downVote
          )
          recommendationDao.update(recommendationId, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    }

  def downVote(recommendationId: String, alreadyVoted: Boolean)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[Recommendation]]] =
    if (alreadyVoted) {
      val recommendationInfo = recommendationDao.get(recommendationId)
      recommendationInfo.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = recommendationWithId.entity.approved,
            decline = recommendationWithId.entity.decline,
            pending = recommendationWithId.entity.pending,
            done = recommendationWithId.entity.done,
            book = recommendationWithId.entity.book,
            upVote = recommendationWithId.entity.upVote - 1,
            downVote = recommendationWithId.entity.downVote + 1
          )
          recommendationDao.update(recommendationId, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    } else {
      val recommendationInfo = recommendationDao.get(recommendationId)
      recommendationInfo.flatMap {
        case Some(recommendationWithId) =>
          val recommendation = Recommendation(
            email = recommendationWithId.entity.email,
            name = recommendationWithId.entity.name,
            topic = recommendationWithId.entity.topic,
            description = recommendationWithId.entity.description,
            submissionDate = recommendationWithId.entity.submissionDate,
            updateDate = new Date,
            approved = recommendationWithId.entity.approved,
            decline = recommendationWithId.entity.decline,
            pending = recommendationWithId.entity.pending,
            done = recommendationWithId.entity.done,
            book = recommendationWithId.entity.book,
            upVote = recommendationWithId.entity.upVote,
            downVote = recommendationWithId.entity.downVote + 1
          )
          recommendationDao.update(recommendationId, _ => recommendation).map {
            case Some(result) => result.asRight
            case None => RecommendationServiceError.RecommendationError.asLeft
          }
        case None =>
          Future.successful(RecommendationServiceError.RecommendationNotFoundError.asLeft)
      }
    }

  def insertResponse(
    recommendationResponse: RecommendationResponse
  )(implicit
    authorityUser: NewUserInformation
  ): Future[Either[RecommendationServiceError, WithId[RecommendationsResponse]]] = {
    val response = RecommendationsResponse(
      recommendationResponse.email,
      recommendationResponse.recommendationId,
      recommendationResponse.upVote,
      recommendationResponse.downVote
    )
    val filter = And(RecommendationId(response.recommendationId), Email(response.email))
    val data = recommendationsResponseDao.get(filter)
    data.flatMap {
      case Some(value) =>
        val recommendation = RecommendationsResponse(
          email = value.entity.email,
          recommendationId = value.entity.recommendationId,
          upVote = response.upVote,
          downVote = response.downVote
        )
        recommendationsResponseDao.update(value.id, _ => recommendation).map {
          case Some(result) => result.asRight
          case None => RecommendationServiceError.RecommendationError.asLeft
        }
      case None =>
        recommendationsResponseDao.create(response).map { id =>
          val recommendationResponse = WithId(response, id)
          recommendationResponse.asRight
        }
    }
  }

}

object RecommendationService {

  sealed trait RecommendationServiceError

  object RecommendationServiceError {

    case object RecommendationError extends RecommendationServiceError

    case object RecommendationAccessDeniedError extends RecommendationServiceError

    case object RecommendationNotFoundError extends RecommendationServiceError

    case object InvalidRecommendationError extends RecommendationServiceError

  }

}
