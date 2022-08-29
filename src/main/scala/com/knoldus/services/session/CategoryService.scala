package com.knoldus.services.session

import akka.event.LoggingAdapter
import cats.implicits._
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, TrueFilter }
import com.knoldus.dao.session.CategoryDao.CategoryName
import com.knoldus.dao.session.SessionDao.{ CategoryCheck, SubCategory }
import com.knoldus.dao.session.{ CategoryDao, SessionDao }
import com.knoldus.domain.session.{ Category, Session }
import com.knoldus.domain.user.KeycloakRole.Admin
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.contract.session.AddCategoryRequest
import com.knoldus.services.session.CategoryService.CategoryServiceError

import scala.concurrent.{ ExecutionContext, Future }

class CategoryService(categoryDao: CategoryDao, sessionDao: SessionDao)(implicit
  val ec: ExecutionContext,
  val logger: LoggingAdapter
) {

  def getPrimaryCategories: Future[Either[CategoryServiceError, Seq[WithId[Category]]]] = {
    val categories = categoryDao.listAll(TrueFilter)
    categories.map { primaryCategory =>
      primaryCategory.asRight
    }
  }

  def modifyPrimaryCategory(categoryId: String, newCategoryName: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[CategoryServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin) categoryDao.get(categoryId).flatMap {
      case categoryInfo if categoryInfo.isEmpty =>
        Future.successful(CategoryServiceError.PrimaryCategoryNotFoundError.asLeft)
      case categoryInfo =>
        val category = Category(newCategoryName, categoryInfo.get.entity.subCategory)
        updateCategory(categoryInfo.get.entity.categoryName, newCategoryName)
        categoryDao.update(categoryId, updater(category))
        Future.successful(().asRight)
    }
    else
      Future.successful(CategoryServiceError.AccessDenied.asLeft)

  def deletePrimaryCategory(
    categoryId: String
  )(implicit authorityUser: NewUserInformation): Future[Either[CategoryServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin)
      categoryDao.get(categoryId).flatMap {
        case categoryInfo if categoryInfo.isEmpty =>
          Future.successful(CategoryServiceError.PrimaryCategoryNotFoundError.asLeft)
        case categoryInfo if categoryInfo.get.entity.subCategory.nonEmpty =>
          Future.successful(CategoryServiceError.PrimaryCategoryDeleteError.asLeft)
        case categoryInfo =>
          categoryDao.delete(categoryId)
          updateCategory(categoryInfo.get.entity.categoryName, "")
          Future.successful(().asRight)
      }
    else
      Future.successful(CategoryServiceError.AccessDenied.asLeft)

  def addPrimaryCategory(
    request: AddCategoryRequest
  )(implicit authorityUser: NewUserInformation): Future[Either[CategoryServiceError, String]] =
    if (authorityUser.keycloakRole == Admin)
      categoryDao.listAll(TrueFilter).flatMap {
        case categories
            if !categories.exists(
              _.entity.categoryName.toLowerCase == request.categoryName.toLowerCase
            ) && authorityUser.keycloakRole == Admin =>
          categoryDao.create(Category(request.categoryName, request.subCategory)).map(categoryId => categoryId.asRight)
        case categories if !categories.exists(_.entity.categoryName.toLowerCase == request.categoryName.toLowerCase) =>
          Future.successful(CategoryServiceError.AccessDenied.asLeft)
        case categories if categories.exists(_.entity.categoryName.toLowerCase == request.categoryName.toLowerCase) =>
          Future.successful(CategoryServiceError.PrimaryCategoryAlreadyExistError.asLeft)
      }
    else
      Future.successful(CategoryServiceError.AccessDenied.asLeft)

  def getSubCategoryByPrimaryCategory(categoryName: String): Future[Either[CategoryServiceError, WithId[Category]]] =
    categoryDao.get(CategoryName(categoryName)).flatMap {
      case category if category.isEmpty => Future.successful(CategoryServiceError.PrimaryCategoryNotFoundError.asLeft)
      case category =>
        val categoryInfo = category.get
        Future.successful(categoryInfo.asRight)
    }

  def addSubCategory(
    categoryId: String,
    subCategory: String
  )(implicit authorityUser: NewUserInformation): Future[Either[CategoryServiceError, WithId[Category]]] =
    if (authorityUser.keycloakRole == Admin)
      categoryDao.get(categoryId).flatMap {
        case categoryInfo if categoryInfo.isEmpty =>
          Future.successful(CategoryServiceError.PrimaryCategoryNotFoundError.asLeft)
        case categoryInfo if categoryInfo.get.entity.subCategory.exists(_.toLowerCase == subCategory.toLowerCase) =>
          Future.successful(CategoryServiceError.SubCategoryAlreadyExistError.asLeft)
        case categoryInfo =>
          val subCategories = categoryInfo.get.entity.subCategory
          val category = Category(categoryInfo.get.entity.categoryName, subCategories.:+(subCategory))
          val updatedCategory = categoryDao.update(categoryId, updater(category)).map(_.get)
          updatedCategory.map { category =>
            category.asRight
          }
      }
    else
      Future.successful(CategoryServiceError.AccessDenied.asLeft)

  def modifySubCategory(
    categoryId: String,
    oldSubCategory: String,
    newSubCategory: String
  )(implicit authorityUser: NewUserInformation): Future[Either[CategoryServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin)
      categoryDao.get(categoryId).flatMap {
        case categoryInfo if !categoryInfo.get.entity.subCategory.exists(_.toLowerCase == oldSubCategory.toLowerCase) =>
          Future.successful(CategoryServiceError.SubCategoryNotFoundError.asLeft)
        case categoryInfo if categoryInfo.get.entity.subCategory.exists(_.toLowerCase == newSubCategory.toLowerCase) =>
          Future.successful(CategoryServiceError.SubCategoryAlreadyExistError.asLeft)
        case categoryInfo =>
          val oldSubCategories = categoryInfo.get.entity.subCategory.diff(Seq(oldSubCategory))
          val newSubCategories = oldSubCategories.:+(newSubCategory)
          val category = Category(categoryInfo.get.entity.categoryName, newSubCategories)
          updateSubCategory(oldSubCategory, newSubCategory)
          categoryDao.update(categoryId, updater(category))
          Future.successful(().asRight)
      }
    else
      Future.successful(CategoryServiceError.AccessDenied.asLeft)

  def deleteSubCategory(categoryId: String, subCategory: String)(implicit
    authorityUser: NewUserInformation
  ): Future[Either[CategoryServiceError, Unit]] =
    if (authorityUser.keycloakRole == Admin)
      categoryDao.get(categoryId).flatMap {
        case categoryInfo if categoryInfo.isEmpty =>
          Future.successful(CategoryServiceError.PrimaryCategoryNotFoundError.asLeft)
        case categoryInfo if !categoryInfo.get.entity.subCategory.exists(_.toLowerCase == subCategory.toLowerCase) =>
          Future.successful(CategoryServiceError.SubCategoryNotFoundError.asLeft)
        case categoryInfo =>
          val newSubCategory = categoryInfo.get.entity.subCategory.diff(Seq(subCategory))
          val category = Category(categoryInfo.get.entity.categoryName, newSubCategory)
          updateSubCategory(subCategory, "")
          categoryDao.update(categoryId, updater(category))
          Future.successful(().asRight)
      }
    else
      Future.successful(CategoryServiceError.AccessDenied.asLeft)

  def getTopicsBySubCategory(
    categoryName: String,
    subCategory: String
  ): Future[Either[CategoryServiceError, Seq[WithId[Session]]]] =
    sessionDao.listAll(And(CategoryCheck(categoryName), SubCategory(subCategory))).map { sessionInfo =>
      sessionInfo.asRight
    }

  private def updateCategory(oldCategory: String, newCategory: String) = {
    val sessions = sessionDao.listAll(CategoryCheck(oldCategory))
    sessions.map { session =>
      if (session.isEmpty)
        "No sessions to update"
      else
        session.map { sessionInfo =>
          val session = Session(
            sessionInfo.entity.userId,
            sessionInfo.entity.email,
            sessionInfo.entity.date,
            sessionInfo.entity.session,
            newCategory,
            sessionInfo.entity.subCategory,
            sessionInfo.entity.feedbackFormId,
            sessionInfo.entity.topic,
            sessionInfo.entity.feedbackExpirationDays,
            sessionInfo.entity.meetup,
            sessionInfo.entity.brief,
            sessionInfo.entity.rating,
            sessionInfo.entity.score,
            sessionInfo.entity.cancelled,
            sessionInfo.entity.active,
            sessionInfo.entity.expirationDate,
            sessionInfo.entity.youtubeURL,
            sessionInfo.entity.slideShareURL,
            sessionInfo.entity.temporaryYoutubeURL,
            sessionInfo.entity.reminder,
            sessionInfo.entity.notification
          )
          sessionDao.updateMany(CategoryCheck(oldCategory), sessionUpdater(session))
        }
    }
  }

  private def updateSubCategory(oldSubCategory: String, newSubCategory: String) = {
    val sessions = sessionDao.listAll(SubCategory(oldSubCategory))
    sessions.map { session =>
      if (session.isEmpty)
        "No sessions to update"
      else
        session.map { sessionInfo =>
          val session = Session(
            sessionInfo.entity.userId,
            sessionInfo.entity.email,
            sessionInfo.entity.date,
            sessionInfo.entity.session,
            sessionInfo.entity.category,
            newSubCategory,
            sessionInfo.entity.feedbackFormId,
            sessionInfo.entity.topic,
            sessionInfo.entity.feedbackExpirationDays,
            sessionInfo.entity.meetup,
            sessionInfo.entity.brief,
            sessionInfo.entity.rating,
            sessionInfo.entity.score,
            sessionInfo.entity.cancelled,
            sessionInfo.entity.active,
            sessionInfo.entity.expirationDate,
            sessionInfo.entity.youtubeURL,
            sessionInfo.entity.slideShareURL,
            sessionInfo.entity.temporaryYoutubeURL,
            sessionInfo.entity.reminder,
            sessionInfo.entity.notification
          )
          sessionDao.updateMany(SubCategory(oldSubCategory), sessionUpdater(session))
        }
    }
  }

  def updater(category: Category): Category => Category = { _: Category =>
    category
  }

  def sessionUpdater(session: Session): Session => Session = { _: Session =>
    session
  }
}

object CategoryService {

  sealed trait CategoryServiceError

  object CategoryServiceError {

    case object PrimaryCategoryNotFoundError extends CategoryServiceError
    case object SubCategoryNotFoundError extends CategoryServiceError
    case object PrimaryCategoryAlreadyExistError extends CategoryServiceError
    case object SubCategoryAlreadyExistError extends CategoryServiceError
    case object PrimaryCategoryDeleteError extends CategoryServiceError
    case object AccessDenied extends CategoryServiceError

  }
}
