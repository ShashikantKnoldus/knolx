package com.knoldus.services.info

import com.knoldus.BaseSpec
import com.knoldus.bootstrap.DaoInstantiator
import com.knoldus.dao.filters.Filter
import com.knoldus.dao.user.UserDao
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future

class InfoServiceSpec extends BaseSpec {

  trait Setup {
    val userDao: UserDao = mock[UserDao]
    val daoInstantiator: DaoInstantiator = mock[DaoInstantiator]
    val service = new InfoService(conf, daoInstantiator)(ec, logger)
  }

  "InfoService#getUpTime" should {

    "get uptime" in new Setup {
      whenReady(service.getUptime)(_.uptime.toMillis should be > 0L)
    }

  }
  "InfoService#getDbStatus" should {

    "return active DbStatus" in new Setup {
      when(daoInstantiator.userDao).thenReturn(userDao)
      when(daoInstantiator.userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future(1))

      assert(service.getDbStatus)
    }

    "return inactive DbStatus" in new Setup {
      when(daoInstantiator.userDao).thenReturn(userDao)
      when(daoInstantiator.userDao.count(any[Filter])(any[concurrent.ExecutionContext])).thenReturn(Future.never)

      assert(!service.getDbStatus)

    }
  }
}
