package com.knoldus.routes.session

import akka.http.scaladsl.model.{ StatusCode, StatusCodes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.knoldus.domain.user.NewUserInformation
import com.knoldus.routes.AuthorizationRoutes
import com.knoldus.routes.contract.common.{ ErrorResponse, IdResponse }
import com.knoldus.routes.contract.session._
import com.knoldus.services.session.CategoryService
import com.knoldus.services.session.CategoryService.CategoryServiceError
import com.knoldus.services.usermanagement.AuthorizationService

class CategoryRoutes(service: CategoryService, val authorizationService: AuthorizationService)
    extends AuthorizationRoutes {

  val routes: Route =
    pathPrefix("category") {
      pathEnd {
        get {
          onSuccess(service.getPrimaryCategories) {
            case Right(categories) =>
              complete(GetAllCategoryResponse.fromDomain(categories))
            case Left(error) => complete(translateError(error))
          }
        } ~
          (post & entity(as[AddCategoryRequest])) { request =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.addPrimaryCategory(request)) {
                case Right(category) => complete(IdResponse(category))
                case Left(error) => complete(translateError(error))
              }
            }
          }
      } ~
        path(Segment) { categoryId =>
          (put & entity(as[UpdatePrimaryCategoryRequest])) { request =>
            authenticationWithBearerToken { authParams =>
              implicit val user: NewUserInformation = authParams
              onSuccess(service.modifyPrimaryCategory(categoryId, request.newCategoryName)) {
                case Right(_) => complete(IdResponse(categoryId))
                case Left(error) => complete(translateError(error))
              }
            }
          } ~
            delete {
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                onSuccess(service.deletePrimaryCategory(categoryId)) {
                  case Right(_) => complete(IdResponse(categoryId))
                  case Left(error) => complete(translateError(error))
                }
              }
            }
        }
    } ~
        pathPrefix("subcategory") {
          pathEnd {
            (get & parameters("categoryName".as[String], "subCategory".?)) { (categoryName, subCategory) =>
              if (subCategory.isDefined)
                onSuccess(service.getTopicsBySubCategory(categoryName, subCategory.get)) {
                  case Right(categories) => complete(GetTopicsBySubCategoryResponse.fromDomain(categories))
                  case Left(error) => complete(translateError(error))
                }
              else
                onSuccess(service.getSubCategoryByPrimaryCategory(categoryName)) {
                  case Right(categories) => complete(GetSubCategoryByCategoryResponse.fromDomain(categories))
                  case Left(error) => complete(translateError(error))
                }
            } ~
              (post & entity(as[AddSubCategoryRequest])) { request =>
                authenticationWithBearerToken { authParams =>
                  implicit val user: NewUserInformation = authParams
                  onSuccess(service.addSubCategory(request.categoryId, request.subCategory)) {
                    case Right(category) => complete(AddCategoryResponse.fromDomain(category))
                    case Left(error) => complete(translateError(error))
                  }
                }
              }
          } ~
            path(Segment) { categoryId =>
              authenticationWithBearerToken { authParams =>
                implicit val user: NewUserInformation = authParams
                (put & entity(as[UpdateSubcategoryRequest])) { request =>
                  onSuccess(service.modifySubCategory(categoryId, request.oldSubCategory, request.newSubCategory)) {
                    case Right(_) => complete(IdResponse(categoryId))
                    case Left(error) => complete(translateError(error))
                  }
                }
              } ~
                authenticationWithBearerToken { authParams =>
                  implicit val user: NewUserInformation = authParams
                  (delete & parameters("subCategory".as[String])) { subCategory =>
                    onSuccess(service.deleteSubCategory(categoryId, subCategory)) {
                      case Right(_) => complete(IdResponse(categoryId))
                      case Left(error) => complete(translateError(error))
                    }
                  }
                }
            }
        }

  def translateError(error: CategoryServiceError): (StatusCode, ErrorResponse) =
    error match {
      case CategoryServiceError.PrimaryCategoryNotFoundError =>
        errorResponse(StatusCodes.NotFound, "Primary Category Not Found")
      case CategoryServiceError.SubCategoryNotFoundError =>
        errorResponse(StatusCodes.NotFound, "SubCategory Not Found")
      case CategoryServiceError.PrimaryCategoryAlreadyExistError =>
        errorResponse(StatusCodes.BadRequest, "Primary Category Already Exist")
      case CategoryServiceError.SubCategoryAlreadyExistError =>
        errorResponse(StatusCodes.BadRequest, "subCategory Already Exist")
      case CategoryServiceError.PrimaryCategoryDeleteError =>
        errorResponse(
          StatusCodes.BadRequest,
          "All sub categories should be deleted prior to deleting the primary category"
        )
      case CategoryServiceError.AccessDenied =>
        errorResponse(StatusCodes.Unauthorized, "Access Denied")
    }
}
