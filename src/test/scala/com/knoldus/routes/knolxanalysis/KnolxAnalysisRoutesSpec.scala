package com.knoldus.routes.knolxanalysis

import java.util.Date
import org.mockito.ArgumentMatchers.any
import com.knoldus.RandomGenerators
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.session.{ CategoryDao, SessionDao }
import com.knoldus.domain.session.Session
import com.knoldus.routes.RoutesSpec
import com.knoldus.routes.contract.common.ErrorResponse
import com.knoldus.routes.contract.knolxanalysis.{ CategoryInformation, KnolxMonthlyInfo, SubCategoryInformation }
import com.knoldus.routes.contract.session.GetSessionByIdResponse
import com.knoldus.services.knolxanalysis.KnolxAnalysisService
import com.knoldus.services.knolxanalysis.KnolxAnalysisService.KnolxAnalysisServiceError

import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

class KnolxAnalysisRoutesSpec extends RoutesSpec {

  trait Setup extends RoutesSetup {
    val service: KnolxAnalysisService = mock[KnolxAnalysisService]
    val sessionDao: SessionDao = mock[SessionDao]
    val categoryDao: CategoryDao = mock[CategoryDao]
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val randomBoolean: Boolean = RandomGenerators.randomBoolean()
    val routes: Route = new KnolxAnalysisRoutes(service).routes
    val error: KnolxAnalysisServiceError = mock[KnolxAnalysisServiceError]
    val randomId: String = randomString
    val dateTest: Date = new Date(1575927000000L)
    val startDate: String = randomString
    val endDate: String = randomString
    val testEmail: Option[String] = Some("test")
    val dateInLong: Long = 123456789
    val longDate = 1234696d

    val categoryInfo: CategoryInformation =
      CategoryInformation(randomString, randomInt, List(SubCategoryInformation(randomString, randomInt)))
    val knolxMonthlyInfo: KnolxMonthlyInfo = KnolxMonthlyInfo(randomString, randomInt)
    val knolxRoutes: KnolxAnalysisRoutes = new KnolxAnalysisRoutes(service)

    val getSessionByIdResponse: GetSessionByIdResponse = GetSessionByIdResponse(
      randomString,
      randomString,
      randomString,
      new Date(),
      randomString,
      randomString,
      randomString,
      randomString,
      randomString,
      randomString,
      1,
      meetup = true,
      randomString,
      1.0,
      cancelled = false,
      active = true,
      dateTest,
      randomString,
      randomString,
      Some(randomString),
      reminder = true,
      notification = true,
      randomString
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
    val withId: WithId[Session] = WithId(entity = sessionInfo, randomId)
  }

  "Get /knolxanalysis/columnchart endpoint" should {

    "return session in given time range " in new Setup {

      service.getSessionInRange(any[String], any[String]) shouldReturn Future(Right(Seq(withId)))
      Get(s"/knolxanalysis/columnchart?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /knolxanalysis/columnchart endpoint" should {

    "return error " in new Setup {

      service.getSessionInRange(any[String], any[String]) shouldReturn Future(Left(error))
      Get(s"/knolxanalysis/columnchart?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /knolxanalysis/piechart endpoint" should {

    "return categoryInformation in given range " in new Setup {

      service.doCategoryAnalysis(any[String], any[String]) shouldReturn Future(Right(Tuple2(List(categoryInfo), 1)))
      Get(s"/knolxanalysis/piechart?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /knolxanalysis/piechart endpoint" should {

    "return error " in new Setup {

      service.doCategoryAnalysis(any[String], any[String]) shouldReturn Future(Left(error))
      Get(s"/knolxanalysis/piechart?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Get /knolxanalysis/linechart endpoint" should {

    "return monthly knolx information " in new Setup {

      service.doKnolxMonthlyAnalysis(any[String], any[String]) shouldReturn Future(Right(List(knolxMonthlyInfo)))
      Get(s"/knolxanalysis/linechart?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Get /knolxanalysis/linechart endpoint" should {

    "return error " in new Setup {

      service.doKnolxMonthlyAnalysis(any[String], any[String]) shouldReturn Future(Left(error))
      Get(s"/knolxanalysis/linechart?startDate=$startDate&endDate=$endDate").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }

  "Put/knolxanalysis/session in time range check error" should {

    "return error " in new Setup {

      service.sessionsInTimeRange(any[Some[String]], any[Date], any[Date]) shouldReturn Future(Left(error))
      Put(s"/knolxanalysis/session-in-time-range?email=$testEmail&startDate=$dateInLong&endDate=$dateInLong").check {
        status shouldBe StatusCodes.InternalServerError
      }
    }
  }
  "Put/knolxanalysis/session in time range" should {

    "list of sessions" in new Setup {

      service.sessionsInTimeRange(any[Some[String]], any[Date], any[Date]) shouldReturn Future(
            Right(List(getSessionByIdResponse))
          )
      Put(s"/knolxanalysis/session-in-time-range?email=$testEmail&startDate=$dateInLong&endDate=$dateInLong").check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "Check Translate error for SessionNotFound " should {
    "return translate error" in new Setup {
      knolxRoutes.translateError(KnolxAnalysisServiceError.SessionsNotFoundError) shouldBe
          Tuple2(StatusCodes.NotFound, ErrorResponse(404, "No Sessions Found", None, List()))
    }
  }
}
