package com.knoldus

import com.knoldus.dao.clientsecrets.ClientSecretsDao
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.TrueFilter
import com.knoldus.domain.clients.Client
import org.mockito.Mockito.when

import scala.concurrent.Future

class ClientCredentilaSpec extends BaseSpec {

  trait Setup {
    val clientCredentialDao: ClientSecretsDao = mock[ClientSecretsDao]
    val client = new ClientCredential(clientCredentialDao)
    val randomString: String = RandomGenerators.randomString()
    val id: String = "ID"
    val secret: String = "Secret"
    val clientInfo: Client = Client(id, secret)
    val number = 12
  }

  "credential" should {
    "check credential " in new Setup {
      when(clientCredentialDao.listAll(TrueFilter)).thenReturn((Future(Seq(WithId(clientInfo, "123")))))
      val result = client.storeCredentialsInCache(clientCache)
      result.map { response =>
        assert(response.nonEmpty)
      }
    }
  }
}
