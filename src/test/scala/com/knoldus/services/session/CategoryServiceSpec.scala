package com.knoldus.services.session

import java.util.Date
import com.knoldus.{ BaseSpec, RandomGenerators }
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.{ CategoryDao, SessionDao }
import com.knoldus.dao.session.CategoryDao.{ CategoryName, ListAll }
import com.knoldus.dao.session.SessionDao.{ CategoryCheck, SubCategory }
import com.knoldus.dao.sorting.SortBy
import com.knoldus.domain.session.{ Category, Session }
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.routes.contract.session.AddCategoryRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.{ ExecutionContext, Future }

class CategoryServiceSpec extends BaseSpec {

  trait Setup {
    val categoryDao: CategoryDao = mock[CategoryDao]
    val sessionDao: SessionDao = mock[SessionDao]
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val category: Category = Category("Primary", Seq("Mysql"))
    val categoryTest: Category = Category("primary", Seq())
    val addCategoryRequest: AddCategoryRequest = AddCategoryRequest("random", Seq("random"))
    val withId: WithId[Category] = WithId(category, "1")
    val withIdTest: WithId[Category] = WithId(categoryTest, "1")
    val service = new CategoryService(categoryDao, sessionDao)
    val dateTest: Date = new Date(1575927000000L)
    val testEmail = "test"
    val testName = "Testing"

    implicit val newUserInformationAdmin: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Admin
      )

    implicit val newUserInformationEmployee: NewUserInformation =
      NewUserInformation(
        testEmail,
        testName,
        Employee
      )

    implicit val userInformation: UserInformation =
      UserInformation(
        testEmail,
        active = true,
        admin = true,
        coreMember = true,
        superUser = true,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )

    implicit val userInformationTest: UserInformation =
      UserInformation(
        testEmail,
        active = true,
        admin = true,
        coreMember = true,
        superUser = false,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )

    implicit val userInformationTesting: UserInformation =
      UserInformation(
        testEmail,
        active = true,
        admin = false,
        coreMember = true,
        superUser = false,
        new Date(),
        1,
        lastBannedOn = Some(dateTest),
        nonParticipating = false,
        department = None
      )

    val sessionInfo: Session =
      Session(
        userId = randomString,
        email = randomString,
        dateTest,
        session = randomString,
        category = randomString,
        subCategory = randomString,
        feedbackFormId = randomString,
        topic = randomString,
        feedbackExpirationDays = randomInt,
        meetup = randomBoolean,
        brief = randomString,
        rating = randomString,
        score = 0.0,
        cancelled = randomBoolean,
        active = randomBoolean,
        dateTest,
        youtubeURL = Some(randomString),
        slideShareURL = Some(randomString),
        temporaryYoutubeURL = Some(randomString),
        reminder = randomBoolean,
        notification = randomBoolean
      )
    val withIdSession: WithId[Session] = WithId(entity = sessionInfo, randomString)
  }

  "CategoryService#GetPrimaryCategories" should {

    "return all categories" in new Setup {
      when(categoryDao.listAll(CategoryName(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withId)))
      whenReady(service.getPrimaryCategories) { result =>
        assert(result.isRight)
      }
    }
  }

  "CategoryService#AddPrimaryCategories" should {

    "add given category" in new Setup {
      when(categoryDao.listAll(ListAll(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withId)))
      when(categoryDao.create(any[Category])(any[ExecutionContext])).thenReturn(Future("1"))
      whenReady(service.addPrimaryCategory(addCategoryRequest)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error on adding a given category" in new Setup {
      when(categoryDao.listAll(ListAll(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withId)))
      when(categoryDao.create(any[Category])(any[ExecutionContext])).thenReturn(Future("1"))
      whenReady(service.addPrimaryCategory(addCategoryRequest)(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CategoryService#DeletePrimaryCategories" should {
    "delete given category" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withIdTest)))
      when(categoryDao.delete(any[String])(any[ExecutionContext])).thenReturn(Future(true))
      whenReady(service.deletePrimaryCategory(randomString)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in deleting given category" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withIdTest)))
      when(categoryDao.delete(any[String])(any[ExecutionContext])).thenReturn(Future(true))
      whenReady(service.deletePrimaryCategory(randomString)(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CategoryService#DeletePrimaryCategories" should {
    "return primary category delete error" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.delete(any[String])(any[ExecutionContext])).thenReturn(Future(true))
      whenReady(service.deletePrimaryCategory("1")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#DeletePrimaryCategories" should {
    "don't delete given category" in new Setup {
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(categoryDao.delete(any[String])(any[ExecutionContext])).thenReturn(Future(false))
      whenReady(service.deletePrimaryCategory("1")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#ModifyPrimaryCategories" should {
    "update given category" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.modifyPrimaryCategory("1", "random")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in update a given category" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.modifyPrimaryCategory("1", "random")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CategoryService#ModifyPrimaryCategories" should {

    "don't update given category" in new Setup {

      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.modifyPrimaryCategory("1", "random")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#GetSubCategoryByPrimaryCategory" should {

    "return all subCategory of given category" in new Setup {

      when(categoryDao.get(CategoryName(any[String]))(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      whenReady(service.getSubCategoryByPrimaryCategory("Database")) { result =>
        assert(result.isRight)
      }
    }
  }

  "CategoryService#GetSubCategoryByPrimaryCategory" should {

    "return category not found error" in new Setup {

      when(categoryDao.get(CategoryName(any[String]))(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.getSubCategoryByPrimaryCategory("Database")) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#AddSubCategory" should {

    "add given subCategory" in new Setup {

      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.addSubCategory("1", "Mongodb")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in adding a given subCategory" in new Setup {

      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.addSubCategory("1", "Mongodb")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CategoryService#AddSubCategory" should {

    "return category not found error" in new Setup {

      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.addSubCategory("1", "Mysql")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#AddSubCategory" should {

    "return subCategory already exist error" in new Setup {

      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.addSubCategory("1", "Mysql")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#ModifySubCategory" should {

    "return subCategory not found error" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.modifySubCategory("1", "Mysql1", "Mongodb")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#ModifySubCategory" should {

    "return subCategory already exist error" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.modifySubCategory("1", "Mysql", "Mysql")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#ModifySubCategory" should {

    "update given subCategory" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.modifySubCategory("1", "Mysql", "Mongodb")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error in subCategory updating" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Option(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.modifySubCategory("1", "Mysql1", "Mongodb")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CategoryService#DeleteSubCategory" should {

    "delete given subCategory" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.deleteSubCategory("1", "Mysql")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "return error on delete a given subCategory" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.deleteSubCategory("1", "Mysql")(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "CategoryService#DeleteSubCategory" should {

    "return subCategory not found error" in new Setup {
      when(sessionDao.listAll(CategoryCheck(any[String]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(Some(withId)))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.deleteSubCategory("1", "Mysql1")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#DeleteSubCategory" should {

    "return category not found error " in new Setup {

      when(categoryDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      when(categoryDao.update(any[String], any[Category => Category].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Option(withId)))
      whenReady(service.deleteSubCategory("1", "Mysql")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "CategoryService#GetTopicsBySubcategory" should {

    "return topics of sessions of given subCategory  " in new Setup {

      when(sessionDao.listAll(And(CategoryCheck(any[String]), SubCategory(any[String])))(any[ExecutionContext]))
        .thenReturn(Future(Seq(withIdSession)))
      whenReady(service.getTopicsBySubCategory("Database", "Mysql")) { result =>
        assert(result.isRight)
      }
    }
  }

}
