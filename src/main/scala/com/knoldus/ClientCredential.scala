package com.knoldus

import akka.event.LoggingAdapter
import akka.http.caching.scaladsl.Cache
import com.knoldus.dao.clientsecrets.ClientSecretsDao
import com.knoldus.dao.commons.WithId
import com.knoldus.dao.filters.TrueFilter
import com.knoldus.domain.clients.Client

import scala.concurrent.{ ExecutionContext, Future }

class ClientCredential(clientsDao: ClientSecretsDao)(
  implicit val ec: ExecutionContext,
  implicit val logger: LoggingAdapter
) {

  def storeCredentialsInCache(cacheClientsSecrets: Cache[String, String]): Future[Seq[String]] =
    getCredentialsFromDatabase.flatMap { clients =>
      Future.sequence(clients.map { client =>
        cacheClientsSecrets.getOrLoad(client.entity.clientId, _ => Future.successful(client.entity.clientSecret))
      })
    }

  private def getCredentialsFromDatabase: Future[Seq[WithId[Client]]] =
    clientsDao.listAll(TrueFilter)
}
