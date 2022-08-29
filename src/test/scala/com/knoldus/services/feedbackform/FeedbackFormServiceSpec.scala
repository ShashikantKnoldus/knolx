package com.knoldus.services.feedbackform

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormDao
import com.knoldus.dao.feedbackform.FeedbackFormDao.ActiveFeedbackForms
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.Active
import com.knoldus.dao.sorting.SortBy
import com.knoldus.domain.feedbackform.{ FeedbackForm, FeedbackFormList, Question }
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class FeedbackFormServiceSpec extends BaseSpec {

  trait Setup {
    val feedbackFormDao: FeedbackFormDao = mock[FeedbackFormDao]
    val sessionDao: SessionDao = mock[SessionDao]
    val service = new FeedbackFormService(feedbackFormDao, sessionDao)
    val feedbackFormServiceMock: FeedbackFormService = mock[FeedbackFormService]
    val randomId: String = RandomGenerators.randomString()
    val randomInteger: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
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

    val feedbackFormInfo: FeedbackForm =
      FeedbackForm(
        "Form",
        List(Question("question", List("1"), "MCQ", mandatory = randomBoolean)),
        active = randomBoolean
      )

    val feedbackFormList: FeedbackFormList =
      FeedbackFormList(
        "Form",
        List(Question("question", List("1"), "MCQ", mandatory = randomBoolean)),
        active = randomBoolean,
        "id"
      )
    val dateTest: Date = new Date()

    val sessionInfo: Session =
      Session(
        randomId,
        email = "random.test",
        date = new Date,
        session = "Session1",
        category = "Database",
        subCategory = "Mysql",
        feedbackFormId = "1",
        topic = "CRUD",
        feedbackExpirationDays = randomInteger,
        meetup = randomBoolean,
        brief = "Major Topic",
        rating = "4",
        score = 0.0,
        cancelled = randomBoolean,
        active = randomBoolean,
        dateTest,
        youtubeURL = Some("www.knoldusYoutube.com"),
        slideShareURL = Some("www.KnoldusSlide.com"),
        temporaryYoutubeURL = Some("www.KnoldusTemp.com"),
        reminder = randomBoolean,
        notification = randomBoolean
      )

    val userInfo: UserInformation =
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

  }

  "FeedbackService#createpost " should {
    " create Feedbackform used for post directive" in new Setup {

      when(feedbackFormDao.create(any[FeedbackForm])(any[ExecutionContext])).thenReturn(Future("Created"))
      whenReady(
        service.createFeedbackForm(
          "question",
          List(Question("How are You", List("Fine", "not well"), "MCQ", mandatory = randomBoolean))
        )(newUserInformationAdmin)
      ) { result =>
        assert(result.isRight)
      }
    }
    " Access denied on creating Feedbackform used for post directive" in new Setup {
      when(feedbackFormDao.create(any[FeedbackForm])(any[ExecutionContext])).thenReturn(Future("Created"))

      whenReady(
        service.createFeedbackForm(
          "question",
          List(Question("How are You", List("Fine", "not well"), "MCQ", mandatory = randomBoolean))
        )(newUserInformationEmployee)
      ) { result =>
        assert(!result.isRight)
      }
    }
  }

  "FeedbackService#DeleteFeedabck" should {
    "delete feedback of given id" in new Setup {
      when(feedbackFormDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(feedbackFormInfo, randomId))))
      when(
        feedbackFormDao.update(any[String], any[FeedbackForm => FeedbackForm].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(feedbackFormInfo, randomId))))
      whenReady(service.delete(randomId)(newUserInformationAdmin))(_ should be(Right(())))
    }
    "Return Error in Delete Feedbackform" in new Setup {

      when(feedbackFormDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.delete(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "FeedbackService#update " should {
    " Update Feedbackform used for put directive" in new Setup {
      when(
        feedbackFormDao.get(any[String])(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(feedbackFormInfo, randomId))))
      when(
        feedbackFormDao.update(any[String], any[FeedbackForm => FeedbackForm].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(feedbackFormInfo, randomId))))
      whenReady(
        service.update(
          randomId,
          Some("new name"),
          Some(List(Question("NewQue", List("1", "2"), "MCQ", mandatory = randomBoolean)))
        )(newUserInformationAdmin)
      ) { result =>
        result should be(Right(()))
      }
      when(
        feedbackFormDao.get(any[String])(any[ExecutionContext])
      ).thenReturn(Future(None))
      whenReady(
        service.update(
          randomId,
          None,
          None
        )(newUserInformationAdmin)
      ) { result =>
        result.isLeft
      }
    }
  }

  "FeedbackService#list All FeedbackForms" should {
    "get list of created Feedback form" in new Setup {
      when(
        feedbackFormDao.list(ActiveFeedbackForms(any[Boolean]), any[Int], any[Int], any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Seq(WithId(feedbackFormInfo, randomId))
          )
      when(feedbackFormDao.count(any[Filter])(any[ExecutionContext])) thenReturn Future(1)
      whenReady(service.listAllFeedbackForms(randomInteger)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "access denied in getting list of created Feedback form" in new Setup {
      when(
        feedbackFormDao.list(ActiveFeedbackForms(any[Boolean]), any[Int], any[Int], any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Seq(WithId(feedbackFormInfo, randomId))
          )
      when(feedbackFormDao.count(any[Filter])(any[ExecutionContext])) thenReturn Future(1)
      whenReady(service.listAllFeedbackForms(randomInteger)(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }

  }

  "FeedbackService#get Feedback Form by Id" should {
    "get created Feedback form using Id" in new Setup {

      when(sessionDao.listAll(Active(any[Boolean]), any[Option[SortBy]])(any[ExecutionContext])) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      when(feedbackFormDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(feedbackFormInfo, randomId))))
      whenReady(service.getFeedbackFormById("1")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "get created Feedbackform list using Id for session" in new Setup {

      when(feedbackFormDao.listAll(ActiveFeedbackForms(any[Boolean]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(feedbackFormInfo, randomId))))

      whenReady(service.getAllFeedbackForm(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }

    }
    "user is not authorized for creating a feedbackform list" in new Setup {

      when(feedbackFormDao.listAll(ActiveFeedbackForms(any[Boolean]), any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(feedbackFormInfo, randomId))))

      whenReady(service.getAllFeedbackForm(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }

    }
  }

}
