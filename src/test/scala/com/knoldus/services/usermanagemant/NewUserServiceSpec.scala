package com.knoldus.services.usermanagemant

import com.knoldus.BaseSpec
import com.knoldus.dao.user.UserDao
import com.knoldus.services.IAM.IAMService
import com.knoldus.services.keyCloak.{ KeycloakIAMService, KeycloakInstance }
import com.knoldus.services.usermanagement.NewUserService
import com.knoldus.services.usermanagement.NewUserService.NewUserServiceError
import org.keycloak.admin.client.resource.{ RealmResource, UsersResource }
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.UserRepresentation
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.jdk.CollectionConverters.SeqHasAsJava

class NewUserServiceSpec extends BaseSpec {

  trait Setup {

    val keycloak: Keycloak = mock[Keycloak]
    val keycloakInstance: KeycloakInstance = mock[KeycloakInstance]
    val iamService: IAMService = new KeycloakIAMService(keycloakInstance, conf)
    val userDao: UserDao = mock[UserDao]
    val service = new NewUserService(userDao, conf, iamService)
    val usersResource: UsersResource = mock[UsersResource]
    val realmResource: RealmResource = mock[RealmResource]
    val error: NewUserServiceError = mock[NewUserServiceError]
    val userRepresentation: UserRepresentation = mock[UserRepresentation]
  }
  "NewUserService#getAllUsers" should {

    "return all users" in new Setup {
      when(keycloakInstance.getKeycloakInstance(conf)) thenReturn keycloak
      when(keycloak.realm(any[String])) thenReturn realmResource
      when(realmResource.users()) thenReturn usersResource
      when(usersResource.list()) thenReturn List(userRepresentation).asJava
      whenReady(service.getAllUsers) { result =>
        assert(result.nonEmpty)
      }
    }

    "return error" in new Setup {
      when(keycloakInstance.getKeycloakInstance(conf)) thenReturn keycloak
      when(keycloak.realm(any[String])) thenReturn realmResource
      when(realmResource.users()) thenReturn usersResource
      when(usersResource.list()) thenReturn List[UserRepresentation]().asJava
      whenReady(service.getAllUsers) { result =>
        assert(result.isEmpty)
      }
    }
  }
}
