package com.knoldus.services.feedbackform

import akka.Done
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.feedbackform.FeedbackFormsResponseDao.{ FeedbackResponseScore, StoredSessionId, StoredUserId }
import com.knoldus.dao.feedbackform.{ FeedbackFormDao, FeedbackFormsResponseDao }
import com.knoldus.dao.filters.And
import com.knoldus.dao.session.SessionDao
import com.knoldus.dao.session.SessionDao.{ Active, PresenterSession }
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.CheckBanUser
import com.knoldus.domain.feedbackform.{ FeedbackForm, Question, UserFeedbackResponse }
import com.knoldus.domain.session.Session
import com.knoldus.domain.user.UserInformation
import com.knoldus.routes.contract.feedbackform.{ FeedbackResponse, QuestionResponse }
import com.knoldus.services.email.MailerService
import com.knoldus.services.session.SessionService
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class FeedbackFormResponseServiceSpec extends BaseSpec {

  trait Setup {
    val feedbackFormService: FeedbackFormService = mock[FeedbackFormService]
    val feedbackFormResponseService: FeedbackFormResponseService = mock[FeedbackFormResponseService]
    val feedbackResponseDao: FeedbackFormsResponseDao = mock[FeedbackFormsResponseDao]
    val feedbackFormDao: FeedbackFormDao = mock[FeedbackFormDao]
    val sessionService: SessionService = mock[SessionService]
    val sessionDao: SessionDao = mock[SessionDao]
    val userDao: UserDao = mock[UserDao]
    val mailerService: MailerService = mock[MailerService]
    val randomId: String = RandomGenerators.randomString()
    val randomInteger: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val testEmail = "test"

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

    val service = new FeedbackFormResponseService(
      conf,
      mailerService,
      feedbackResponseDao,
      feedbackFormDao,
      sessionDao,
      userDao
    )
    val feedbackResponse: FeedbackResponse = FeedbackResponse(randomId, randomId, List("s"), 4.4)
    val dateTest: Date = new Date()

    val userInfo: UserInformation = UserInformation(
      "email",
      active = randomBoolean,
      admin = randomBoolean,
      coreMember = randomBoolean,
      superUser = randomBoolean,
      dateTest,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val coreUserInfo: UserInformation = UserInformation(
      "email",
      active = randomBoolean,
      admin = randomBoolean,
      coreMember = true,
      superUser = randomBoolean,
      dateTest,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val feedbackFormData: FeedbackForm =
      FeedbackForm("Form", List(Question("question", List("1"), "MCQ", mandatory = true)), active = true)

    val sessionInfo: Session =
      Session(
        randomId,
        email = "random.test",
        dateTest,
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

    val storedUserResponse: UserFeedbackResponse = UserFeedbackResponse(
      randomId,
      randomId,
      coreMember = randomBoolean,
      "praful.bangar@knoldus.com",
      List(),
      meetup = randomBoolean,
      "admin@knoldus.in",
      new Date(1575417600),
      0.0,
      "session 1",
      "tab",
      dateTest
    )

  }

  "FeedbackFormResponseService#SessionFeedbackResponse " should {

    " checked sanitisedFeedbackResponses " in new Setup {
      val result: Seq[Option[QuestionResponse]] =
        service.sanitizeResponses(Seq(Question("s", List("de"), "MCQ", mandatory = randomBoolean)), List("d"))
      result shouldBe List(None)

    }

    "fetch FeedbackFormData Responses " in new Setup {
      when(feedbackResponseDao.get(And(StoredSessionId(any[String]), StoredUserId(any[String])))) thenReturn future(
            Some(WithId(storedUserResponse, randomId))
          )
      whenReady(service.fetchFeedbackResponse(randomId, randomId)) { result =>
        result.isRight
      }
    }
    "return error in fetching FeedbackFormData Responses " in new Setup {
      when(feedbackResponseDao.get(And(StoredSessionId(any[String]), StoredUserId(any[String])))) thenReturn
          future(None)
      whenReady(service.fetchFeedbackResponse(randomId, randomId)) { result =>
        result.isLeft
      }
    }

    "get Ban users in database " in new Setup {
      when(userDao.listAll(CheckBanUser(any[Date]), any[Option[SortBy]])(any[ExecutionContext])) thenReturn future(
            Seq(WithId(userInfo, randomId))
          )
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(userInfo, randomId))
          )
      whenReady(service.getBanUserDetails(randomId)) { result =>
        result.isRight
      }
    }

    "send email to user " in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      when(mailerService.sendMessage(any[String], any[String], any[String])(any[ExecutionContext]))
        .thenReturn(future(Done))
      whenReady(service.sendEmailToUser(randomId, "abc")) { result =>
        result.isRight
      }
    }

    "update rating if coreMember" in new Setup {
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(coreUserInfo, randomId))
          )
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      val result: Unit =
        service.updateRatingIfCoreMember(randomId, randomId, List(0.0))
      result should be(())
    }

    "get all active form for today with ban user" in new Setup {
      when(userDao.listAll(CheckBanUser(any[Date]), any[Option[SortBy]])(any[ExecutionContext])) thenReturn Future(
            Seq(WithId(userInfo, randomId))
          )
      when(
        sessionDao.listAll(And(Active(any[Boolean]), PresenterSession(any[String])))(any[ExecutionContext])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )
      when(feedbackFormDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(feedbackFormData, randomId))))
      whenReady(service.getFeedbackFormForToday(randomId)) { result =>
        result.isRight
      }
    }

    "get all active form for today" in new Setup {
      when(userDao.listAll(CheckBanUser(any[Date]), any[Option[SortBy]])(any[ExecutionContext])) thenReturn Future(
            Seq(WithId(userInfo, randomId))
          )
      when(
        sessionDao.listAll(And(Active(any[Boolean]), PresenterSession(any[String])))(any[ExecutionContext])
      ) thenReturn future(
            Seq(WithId(sessionInfo, randomId))
          )

      when(feedbackResponseDao.get(And(StoredSessionId(any[String]), StoredUserId(any[String])))) thenReturn Future(
            Some(WithId(storedUserResponse, randomId))
          )
      when(feedbackFormDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(feedbackFormData, randomId))))
      whenReady(service.getFeedbackFormForToday("123")) { result =>
        result.isRight
      }
    }

    "store feedback responses from users with update method" in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      when(feedbackFormDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(feedbackFormData, randomId))))
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(userInfo, randomId))
          )
      when(feedbackResponseDao.get(And(StoredSessionId(any[String]), StoredUserId(any[String])))) thenReturn Future(
            Some(WithId(storedUserResponse, randomId))
          )
      when(
        feedbackResponseDao.update(any[String], any[UserFeedbackResponse => UserFeedbackResponse].apply)(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Some(WithId(storedUserResponse, randomId))
          )
      when(mailerService.sendMessage(any[String], any[String], any[String])(any[ExecutionContext]))
        .thenReturn(future(Done))
      when(
        feedbackResponseDao.listAll(
          And(StoredSessionId(any[String]), FeedbackResponseScore(any[Double])),
          any[Option[SortBy]]
        )
      ) thenReturn Future(
            Seq(WithId(storedUserResponse, randomId))
          )
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(userInfo, randomId))
          )
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      when(sessionDao.update(any[String], any[Session => Session].apply)(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      whenReady(
        service.storeFeedbackResponse(
          randomId,
          randomId,
          randomId,
          List("1"),
          0.0
        )
      ) { result =>
        result.isRight
      }
    }

    "store feedback responses from users for create route" in new Setup {
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      when(feedbackFormDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(feedbackFormData, randomId))))
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(userInfo, randomId))
          )
      when(feedbackResponseDao.get(And(StoredSessionId(any[String]), StoredUserId(any[String])))) thenReturn Future(
            None
          )
      when(feedbackResponseDao.create(any[UserFeedbackResponse])(any[ExecutionContext])) thenReturn Future(
            randomId
          )
      when(mailerService.sendMessage(any[String], any[String], any[String])(any[ExecutionContext]))
        .thenReturn(future(Done))
      when(
        feedbackResponseDao.listAll(
          And(StoredSessionId(any[String]), FeedbackResponseScore(any[Double])),
          any[Option[SortBy]]
        )
      ) thenReturn Future(
            Seq(WithId(storedUserResponse, randomId))
          )
      when(userDao.get(any[String])(any[ExecutionContext])) thenReturn Future(
            Some(WithId(userInfo, randomId))
          )
      when(sessionDao.get(any[String])(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      when(sessionDao.update(any[String], any[Session => Session].apply)(any[ExecutionContext])) thenReturn future(
            Some(WithId(sessionInfo, randomId))
          )
      whenReady(
        service.storeFeedbackResponse(
          randomId,
          randomId,
          randomId,
          List("1"),
          0.0
        )
      ) { result =>
        result.isRight
      }
    }

    "get All Response Emails Per Session" in new Setup {
      when(
        feedbackResponseDao.listAll(
          And(StoredSessionId(any[String]), FeedbackResponseScore(any[Double])),
          any[Option[SortBy]]
        )
      ) thenReturn Future(
            Seq(WithId(storedUserResponse, randomId))
          )
      whenReady(service.getAllResponseEmailsPerSession(randomId)) { result =>
        result.isRight
      }
    }

  }

}
