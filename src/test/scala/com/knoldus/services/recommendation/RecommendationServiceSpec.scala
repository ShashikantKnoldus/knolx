package com.knoldus.services.recommendation

import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.{ And, Filter }
import com.knoldus.dao.recommendation.RecommendationDao._
import com.knoldus.dao.recommendation.RecommendationsResponseDao.{ Email, RecommendationId }
import com.knoldus.dao.recommendation.{ RecommendationDao, RecommendationsResponseDao }
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.domain.recommendation.{ Recommendation, RecommendationsResponse }
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.routes.contract.recommendation.{ RecommendationInformation, RecommendationResponse }
import com.knoldus.services.email.MailerService
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.Date
import scala.concurrent.{ ExecutionContext, Future }

class RecommendationServiceSpec extends BaseSpec {

  trait Setup {
    val recommendationDao: RecommendationDao = mock[RecommendationDao]
    val recommendationsResponseDao: RecommendationsResponseDao = mock[RecommendationsResponseDao]
    val mailerService: MailerService = mock[MailerService]
    val userDao: UserDao = mock[UserDao]

    val service = new RecommendationService(recommendationDao, recommendationsResponseDao, userDao, mailerService)
    val randomId: String = RandomGenerators.randomString()
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val date = new Date
    val dateTest = new Date(1575927000000L)
    val testEmail: String = "test"
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

    val userInfo: UserInformation = UserInformation(
      "test",
      active = true,
      admin = true,
      coreMember = false,
      superUser = true,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )
    val withIdUser: WithId[UserInformation] = WithId(userInfo, randomString)

    val recommendationEntity: Recommendation = Recommendation(
      Some(randomString),
      randomString,
      randomString,
      randomString,
      date,
      date,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      randomBoolean,
      0,
      0
    )

    val recommendationInformation: RecommendationInformation = RecommendationInformation(
      Some(randomString),
      randomString,
      randomString,
      randomString
    )

    val recommendationResponseEntity: RecommendationsResponse =
      RecommendationsResponse(randomString, randomId, randomBoolean, randomBoolean)

    val recommendation: RecommendationInformation =
      RecommendationInformation(Some(randomString), randomString, randomString, randomString)

    val recommendationsResponse: RecommendationResponse =
      RecommendationResponse(randomString, randomId, randomBoolean, randomBoolean)

    val filter: And = And(Approved(false), Declined(false))
  }

  "RecommendationService#addRecommendation " should {
    " Suggest Recommendation used post directive" in new Setup {
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withIdUser)))
      when(recommendationDao.create(any[Recommendation])(any[ExecutionContext])).thenReturn(Future("Suggested"))
      whenReady(service.addRecommendation(recommendation)) { result =>
        assert(result.isRight)
        when(recommendationDao.create(any[Recommendation])(any[ExecutionContext])).thenReturn(Future(""))
        whenReady(service.addRecommendation(recommendation)) { result =>
          result.isLeft
        }
      }
    }
  }

  "RecommendationService#getRecommendationById " should {
    " return recommendation of given id" in new Setup {
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withIdUser)))
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Option(WithId(recommendationEntity, randomId))))
      whenReady(service.getRecommendationById(randomId)) { result =>
        assert(result.isRight)
      }
    }

    " return error in getting a recommendation of given id" in new Setup {
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext])).thenReturn(Future(Seq(withIdUser)))
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.getRecommendationById(randomId)) { result =>
        result.isLeft
      }
    }
  }

  "RecommendationService#approveRecommendation" should {
    "approve recommendation of given id" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(recommendationEntity, randomId))))

      whenReady(service.approveRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }

      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(None))

      whenReady(service.approveRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))

      whenReady(service.approveRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft

        when(recommendationDao.get(any[String])(any[ExecutionContext]))
          .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      }
    }
  }

  "RecommendationService#declineRecommendation" should {
    "decline recommendation of given id" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(recommendationEntity, randomId))))

      whenReady(service.declineRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(None))

      whenReady(service.declineRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))

      whenReady(service.declineRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
    }
  }

  "RecommendationService#pendingRecommendation" should {
    "pending recommendation of given id" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(recommendationEntity, randomId))))

      whenReady(service.pendingRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(None))

      whenReady(service.pendingRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))

      whenReady(service.pendingRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
    }
  }

  "RecommendationService#doneRecommendation" should {
    "done recommendation of given id" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(recommendationEntity, randomId))))

      whenReady(service.doneRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(None))

      whenReady(service.doneRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))

      whenReady(service.doneRecommendation(randomId)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
    }
  }

  "RecommendationService#allPendingRecommendation" should {
    "get all pending recommendation count by filter" in new Setup {
      when(recommendationDao.count(any[Filter])(any[ExecutionContext])) thenReturn Future(1)
      whenReady(service.allPendingRecommendation) { result =>
        assert(result.isRight)
      }
      when(recommendationDao.count(any[Filter])(any[ExecutionContext])) thenReturn Future(0)
      whenReady(service.allPendingRecommendation) { result =>
        result.isLeft
      }

    }
  }

  "RecommendationService#bookRecommendation" should {
    "book recommendation of given id" in new Setup {
      when(recommendationDao.get(any[Filter])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(recommendationEntity, randomId))))

      whenReady(service.bookRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(None))
      whenReady(service.bookRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "RecommendationService#cancelBookedRecommendation" should {
    "book recommendation of given id" in new Setup {
      when(recommendationDao.get(any[Filter])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(Some(WithId(recommendationEntity, randomId))))

      whenReady(service.cancelBookedRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(
        recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext])
      ).thenReturn(Future(None))
      whenReady(service.cancelBookedRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
      when(recommendationDao.get(any[Filter])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.cancelBookedRecommendation(randomId)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "RecommendationService#getAllRecommendation " should {
    "get list of all recommendation" in new Setup {
      when(
        recommendationDao.listAll(any[Filter], any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(Seq(WithId(recommendationEntity, randomId)))
      whenReady(service.getAllRecommendation) { result =>
        assert(result.isRight)
      }
    }

  }

  "RecommendationService#getPendingRecommendation" should {
    "get all pending recommendation" in new Setup {
      when(
        recommendationDao.listAll(Pending(any[Boolean]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Seq(WithId(recommendationEntity, randomId))
          )
      whenReady(service.getPendingRecommendation) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#getApproveRecommendation" should {
    "get all approve recommendation" in new Setup {
      when(
        recommendationDao.listAll(Approved(any[Boolean]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Seq(WithId(recommendationEntity, randomId))
          )
      whenReady(service.getApprovedRecommendation) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#getDeclinedRecommendation" should {
    "get all declined recommendation" in new Setup {
      when(
        recommendationDao.listAll(Declined(any[Boolean]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Seq(WithId(recommendationEntity, randomId))
          )
      whenReady(service.getDeclinedRecommendation) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#getBookdRecommendation" should {
    "get all booked recommendation" in new Setup {
      when(
        recommendationDao.listAll(Book(any[Boolean]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Seq(WithId(recommendationEntity, randomId))
          )
      whenReady(service.getBookedRecommendation) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#getDoneRecommendation" should {
    "get all done recommendation" in new Setup {
      when(
        recommendationDao.listAll(Done(any[Boolean]), any[Option[SortBy]])(
          any[ExecutionContext]
        )
      ) thenReturn Future(
            Seq(WithId(recommendationEntity, randomId))
          )
      whenReady(service.getDoneRecommendation) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#listRecommendation with filter all" should {

    "return all recommendation" in new Setup {
      when(recommendationDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(recommendationEntity, randomId))))
      whenReady(service.listRecommendation("all")) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#listRecommendation with filter pending" should {

    "return pending recommendation" in new Setup {
      when(recommendationDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(recommendationEntity, randomId))))
      whenReady(service.listRecommendation("pending")) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#listRecommendation with filter approved" should {

    "return approved recommendation" in new Setup {
      when(recommendationDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(recommendationEntity, randomId))))
      whenReady(service.listRecommendation("approved")) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#listRecommendation with filter declined" should {

    "return declined recommendation" in new Setup {
      when(recommendationDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(recommendationEntity, randomId))))
      whenReady(service.listRecommendation("decline")) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#listRecommendation with filter book" should {

    "return booked recommendation" in new Setup {
      when(recommendationDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(recommendationEntity, randomId))))
      whenReady(service.listRecommendation("book")) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#listRecommendation with filter done" should {

    "return done recommendation" in new Setup {
      when(recommendationDao.listAll(any[Filter], any[Option[SortBy]])(any[ExecutionContext]))
        .thenReturn(Future(Seq(WithId(recommendationEntity, randomId))))
      whenReady(service.listRecommendation("done")) { result =>
        assert(result.isRight)
      }
    }
  }

  "RecommendationService#getVote for recommendation" should {

    "return vote of recommendation" in new Setup {
      when(
        recommendationsResponseDao.get(And(RecommendationId(any[String]), Email(any[String])))
      ).thenReturn(Future(Option(WithId(recommendationResponseEntity, randomId))))
      whenReady(service.getVote("5e010c50e582e47358888aed", "srg@gmail.com")) { result =>
        assert(result.isRight)
      }

      when(
        recommendationsResponseDao.get(And(RecommendationId(any[String]), Email(any[String])))
      ).thenReturn(Future(None))
      whenReady(service.getVote("5e010c50e582e47358888aed", "srg@gmail.com")) { result =>
        result.isRight
      }
    }
  }

  "RecommendationService#upVote with filter alreadyVoted=true" should {

    "return vote of recommendation" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Option(WithId(recommendationEntity, randomId))))
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      whenReady(service.upVote(randomId, alreadyVoted = true)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(None))

      whenReady(service.upVote(randomId, alreadyVoted = true)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.upVote(randomId, alreadyVoted = true)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
    }
  }

  "RecommendationService#upVote with filter alreadyVoted=false" should {

    "return vote of recommendation" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Option(WithId(recommendationEntity, randomId))))
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      whenReady(service.upVote(randomId, alreadyVoted = false)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(None))

      whenReady(service.upVote(randomId, alreadyVoted = false)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.upVote(randomId, alreadyVoted = false)(newUserInformationAdmin)) { result =>
        result.isLeft
      }

    }
  }

  "RecommendationService#downVote with filter alreadyVoted=true" should {

    "return vote of recommendation" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Option(WithId(recommendationEntity, randomId))))
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      whenReady(service.downVote(randomId, alreadyVoted = true)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(None))
      whenReady(service.downVote(randomId, alreadyVoted = true)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.downVote(randomId, alreadyVoted = true)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
    }
  }

  "RecommendationService#downVote with filter alreadyVoted=false" should {

    "return vote of recommendation" in new Setup {
      when(recommendationDao.get(any[String])(any[ExecutionContext]))
        .thenReturn(Future(Option(WithId(recommendationEntity, randomId))))
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationEntity, randomId))))
      whenReady(service.downVote(randomId, alreadyVoted = false)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
      when(recommendationDao.update(any[String], any[Recommendation => Recommendation].apply)(any[ExecutionContext]))
        .thenReturn(Future(None))
      whenReady(service.downVote(randomId, alreadyVoted = false)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
      when(recommendationDao.get(any[String])(any[ExecutionContext])).thenReturn(Future(None))
      whenReady(service.downVote(randomId, alreadyVoted = false)(newUserInformationAdmin)) { result =>
        result.isLeft
      }
    }
  }

  "RecommendationService#inserting the response" should {

    "return insert Response" in new Setup {
      when(recommendationsResponseDao.get(any[And])(any[ExecutionContext]))
        .thenReturn(Future(Some(WithId(recommendationResponseEntity, randomId))))
      when(
        recommendationsResponseDao.update(any[String], any[RecommendationsResponse => RecommendationsResponse].apply)(
          any[ExecutionContext]
        )
      ).thenReturn(Future(Some(WithId(recommendationResponseEntity, randomId))))
      whenReady(service.insertResponse(recommendationsResponse)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
        when(
          recommendationsResponseDao.update(any[String], any[RecommendationsResponse => RecommendationsResponse].apply)(
            any[ExecutionContext]
          )
        ).thenReturn(Future(None))
        whenReady(service.insertResponse(recommendationsResponse)(newUserInformationAdmin)) { result =>
          result.isLeft
        }
      }
    }

    "return insert Response for none" in new Setup {
      when(recommendationsResponseDao.get(any[And])(any[ExecutionContext])).thenReturn(Future(None))
      when(recommendationsResponseDao.create(any[RecommendationsResponse])(any[ExecutionContext]))
        .thenReturn(Future("12"))
      whenReady(service.insertResponse(recommendationsResponse)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }
}
