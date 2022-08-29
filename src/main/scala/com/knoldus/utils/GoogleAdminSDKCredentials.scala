package com.knoldus.utils

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{ GoogleAuthorizationCodeFlow, GoogleClientSecrets }
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.directory.DirectoryScopes
import com.typesafe.config.ConfigFactory

import java.io._
import java.util
import java.util.Collections

object GoogleAdminSDKCredentials {

  val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance
  val config = ConfigFactory.load()

  /** Directory to store authorization tokens for this application. */
  val TOKENS_DIRECTORY_PATH = "tokens"

  val SCOPES: util.List[String] = Collections.unmodifiableList(
    util.Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, DirectoryScopes.ADMIN_DIRECTORY_USER)
  )
  val CREDENTIALS_FILE_PATH = "/credential.json"

  /**
    * Creates an authorized Credential object.
    *
    * @param HTTP_TRANSPORT The network HTTP Transport.
    * @return An authorized Credential object.
    * @throws IOException If the credentials.json file cannot be found.
    */
  @throws[IOException]
  def getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential = { // Load client secrets.
    val googleConfig = config.getConfig("googleCredential")
    val in = new FileInputStream(new File(googleConfig.getString("pathForGoogleApiCredentialFile")))
    if (in == null)
      throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in))
    // Build flow and trigger user authorization request.
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
      .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
      .setAccessType("offline")
      .build
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build

    new AuthorizationCodeInstalledApp(flow, receiver).authorize("bhavya@knoldus.com")
  }

}
