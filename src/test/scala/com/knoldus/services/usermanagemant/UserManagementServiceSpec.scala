package com.knoldus.services.usermanagemant

import akka.Done
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.sorting.SortBy
import com.knoldus.dao.user.UserDao
import com.knoldus.dao.user.UserDao.{ ActiveEmailCheck, CheckActiveUsers, CheckUnBanUser, EmailCheck }
import com.knoldus.domain.user.KeycloakRole.{ Admin, Employee }
import com.knoldus.domain.user.{ NewUserInformation, UserInformation }
import com.knoldus.routes.contract.usermanagement._
import com.knoldus.services.email.MailerService
import com.knoldus.services.usermanagement.UserManagementService
import com.knoldus.services.usermanagement.utilities.PasswordUtility
import com.knoldus.{ BaseSpec, RandomGenerators }
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import java.util.Date
import scala.concurrent.Future

class UserManagementServiceSpec extends BaseSpec {

  trait Setup {
    val userDao: UserDao = mock[UserDao]
    val mailerService: MailerService = mock[MailerService]
    val passwordUtility: PasswordUtility = mock[PasswordUtility]
    val dateTest = new Date(1575927000000L)
    val randomString: String = RandomGenerators.randomString()
    val randomInt: Int = RandomGenerators.randomInt(10)
    val userManagementService: UserManagementService = mock[UserManagementService]

    val service =
      new UserManagementService(
        userDao,
        passwordUtility,
        conf,
        mailerService
      )

    val testEmail = "test"
    val testName = "Testing"
    val testToken = "test"
    val testPassword = "test1234"

    val checkActiveUsers: CheckActiveUsers = CheckActiveUsers(true)
    val checkUnBanUser: CheckUnBanUser = CheckUnBanUser(new Date())

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
      admin = false,
      coreMember = false,
      superUser = false,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val userInfoTest: UserInformation = UserInformation(
      "test",
      active = true,
      admin = false,
      coreMember = true,
      superUser = false,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val userInfo1: UserInformation = UserInformation(
      "test",
      active = true,
      admin = true,
      coreMember = false,
      superUser = false,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val userInfo2: UserInformation = UserInformation(
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

    val userInfo3: UserInformation = UserInformation(
      "test",
      active = true,
      admin = false,
      coreMember = false,
      superUser = false,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )

    val userInfoTesting: UserInformation = UserInformation(
      "test",
      active = true,
      admin = false,
      coreMember = false,
      superUser = false,
      dateTest,
      randomInt,
      lastBannedOn = Some(dateTest),
      nonParticipating = false,
      department = None
    )
    val withIdUser1: WithId[UserInformation] = WithId(userInfo, randomString)
    val withIdUser2: WithId[UserInformation] = WithId(userInfo1, randomString)
    val withIdUser3: WithId[UserInformation] = WithId(userInfo2, randomString)
    val withIdUser4: WithId[UserInformation] = WithId(userInfo3, randomString)
    val updateUserStatusRequest1 = new UpdateUserStatusRequest(testEmail, true, true, false, true, None)
    val updateUserStatusRequest2 = new UpdateUserStatusRequest(testEmail, true, false, false, true, None)

    val updateUserStatusRequest3 = new UpdateUserStatusRequest(testEmail, true, true, false, true, Some("test1234"))

    val updateUserStatusRequest4 = new UpdateUserStatusRequest(testEmail, true, false, false, true, Some("test1234"))

    val changeParticipantStatusResponse = new ChangeParticipantStatusResponse(testEmail, true)

    val updateUserStatusRequest5 = new UpdateUserStatusRequest(testEmail, false, false, false, false, Some("test1234"))

    val updateDepartmentRequest = new UserDepartmentUpdateRequest("Scala")

  }

  "UserManagementService#delete user" should {

    "return error" in new Setup {
      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      whenReady(service.deleteUser(testEmail)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }

    "delete user" in new Setup {

      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))

      whenReady(service.deleteUser(testEmail)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "not delete if user is not admin or superadmin" in new Setup {

      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))

      whenReady(service.deleteUser(testEmail)(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "UserManagementService#update user" should {

    "return error" in new Setup {
      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      whenReady(service.updateUserStatus(updateUserStatusRequest1)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
    "return error in updating user status" in new Setup {
      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      whenReady(service.updateUserStatus(updateUserStatusRequest5)(newUserInformationEmployee)) { result =>
        assert(result.isLeft)
      }
    }

    "update user when password not given and user not banned" in new Setup {

      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(mailerService.sendMessage(any[String], any[String], any[String])(any[concurrent.ExecutionContext]))
        .thenReturn(future(Done.done()))
      whenReady(service.updateUserStatus(updateUserStatusRequest2)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "update user when password not given and user are banned" in new Setup {

      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(mailerService.sendMessage(any[String], any[String], any[String])(any[concurrent.ExecutionContext]))
        .thenReturn(future(Done.done()))
      whenReady(service.updateUserStatus(updateUserStatusRequest1)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "update user when password is given and user is banned" in new Setup {

      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      whenReady(service.updateUserStatus(updateUserStatusRequest3)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "update user when password is given and not banned" in new Setup {
      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(mailerService.sendMessage(any[String], any[String], any[String])(any[concurrent.ExecutionContext]))
        .thenReturn(future(Done.done()))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      whenReady(service.updateUserStatus(updateUserStatusRequest4)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }
  "UserManagementService#update user department" should {

    "return error" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(None))
      whenReady(service.updateUserDepartment(testEmail, "Scala")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }

    "update user when password not given and user not banned" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      whenReady(service.updateUserDepartment(testEmail, "Scala")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
  }
  "UserManagementService#search users" should {

    "search user for all when email provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(service.searchUsers(pageNumber = 1, email = Some(testEmail))(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "search user for banned when email provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(
        service.searchUsers(pageNumber = 1, filter = "banned", email = Some(testEmail))(newUserInformationAdmin)
      ) { result =>
        assert(result.isRight)
      }
    }

    "search user for allowed when email provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(
        service.searchUsers(pageNumber = 1, filter = "allowed", email = Some(testEmail))(newUserInformationAdmin)
      ) { result =>
        assert(result.isRight)
      }
    }

    "search user for active when email provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(
        service.searchUsers(pageNumber = 1, filter = "active", email = Some(testEmail))(newUserInformationAdmin)
      ) { result =>
        assert(result.isRight)
      }
    }

    "search user for suspended when email provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(
        service.searchUsers(pageNumber = 1, filter = "suspended", email = Some(testEmail))(newUserInformationAdmin)
      ) { result =>
        assert(result.isRight)
      }
    }

    "search all users when email not provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(service.searchUsers(pageNumber = 1)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "search banned users when email not provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(service.searchUsers(pageNumber = 1, filter = "banned")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "search allowed users when email not provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(service.searchUsers(pageNumber = 1, filter = "allowed")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "search active users when email not provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(service.searchUsers(pageNumber = 1, filter = "active")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "search suspended users when email not provided" in new Setup {

      when(userDao.list(any[Filter], any[Int], any[Int], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      when(userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      whenReady(service.searchUsers(pageNumber = 1, filter = "suspended")(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }

    "return error" in new Setup {

      whenReady(service.searchUsers(pageNumber = 1, filter = "test")(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "UserManagementService#get user status" should {

    "return error" in new Setup {
      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      whenReady(service.getUserStatusByEmail(testEmail)(newUserInformationAdmin)) { result =>
        assert(result.isLeft)
      }
    }

    "get user's status" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))

      whenReady(service.getUserStatusByEmail(testEmail)(newUserInformationAdmin)) { result =>
        assert(result.isRight)
      }
    }
    "not get user's status if user is not admin or superuser" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))

      whenReady(service.getUserStatusByEmail(testEmail)(newUserInformationEmployee)) { result =>
        assert(!result.isRight)
      }
    }
  }

  "UserManagementService#get user list" should {

    "get user's list when email not provided" in new Setup {

      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))

      whenReady(service.usersList(None)) { result =>
        assert(result.isRight)
      }
    }

    "get user's list when email provided " in new Setup {

      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))

      whenReady(service.usersList(Some(testEmail))) { result =>
        assert(result.isRight)
      }
    }
  }

  "UserManagementService#getActiveAndUnBannedEmails users" should {

    "return get Active And UnBanned Emails" in new Setup {

      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq(withIdUser1)))
      whenReady(service.getActiveAndUnBannedEmails) { result =>
        assert(result.isRight)
      }
    }

    "return error in get Active And UnBanned Emails" in new Setup {

      when(userDao.listAll(any[Filter], any[Option[SortBy]])(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Seq()))
      whenReady(service.getActiveAndUnBannedEmails) { result =>
        assert(result.isLeft)
      }
    }
  }
  "UserManagementService#getUserInformationByEmail users" should {

    "return user Information by mail" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      whenReady(service.getUserInformationByEmail(testEmail)) { result =>
        assert(result.isRight)
      }
    }

    "return error in user Information by mail" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      whenReady(service.getUserInformationByEmail(testEmail)) { result =>
        assert(result.isLeft)
      }
    }
  }

  "UserManagementService change participant status" should {

    "return update user status" in new Setup {
      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      whenReady(service.changeParticipantStatus(testEmail, changeParticipantStatusResponse)(newUserInformationAdmin)) {
        result =>
          assert(result.isRight)
      }
    }

    "return error update user status" in new Setup {
      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      whenReady(service.changeParticipantStatus(testEmail, changeParticipantStatusResponse)(newUserInformationAdmin)) {
        result =>
          assert(result.isLeft)
      }
    }

    "return error for unauthorised User" in new Setup {
      when(userDao.get(any[ActiveEmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(None))
      whenReady(service.changeParticipantStatus(testEmail, changeParticipantStatusResponse)(newUserInformationAdmin)) {
        result =>
          UserManagementService.UserManagementServiceError.AccessDenied
      }
    }
  }

  "UserManagementService#banUser users" should {

    "return error update ban user" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(None))
      whenReady(service.banUser(testEmail)) { result =>
        assert(!result)
      }
    }

    "return update ban user" in new Setup {

      when(userDao.get(any[EmailCheck])(any[concurrent.ExecutionContext])).thenReturn(Future(Some(withIdUser1)))
      when(userDao.update(any[String], any[UserInformation => UserInformation].apply)(any[concurrent.ExecutionContext]))
        .thenReturn(Future(Some(withIdUser1)))
      whenReady(service.banUser(testEmail)) { result =>
        assert(true)
      }
    }
  }
}
