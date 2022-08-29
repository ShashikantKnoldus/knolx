import sbt._

object Dependencies {

  object Versions {
    // System
    val ScalaVersion = "2.13.8"
    val JDKVersion = "1.8"
    // General
    val TypeSafeConfigVersion = "1.4.2"
    val AkkaVersion = "2.6.19"
    val AkkaHttpVersion = "10.2.9"
    val PlayJsonVersion = "2.9.2"
    val AkkaHttpPlayJsonVersion = "1.39.2"
    val CatsVersion = "2.7.0"
    val MongoDBScalaDriverVersion = "4.6.0"
    val MailVersion = "1.4"
    val rollbarVersion = "1.8.1"
    val sendgridVersion = "4.9.1"
    val keycloakVersion = "18.0.0"
    // Test
    val ScalaTestVersion = "3.2.12"
    val MockitoScalaVersion = "1.17.5"
    val ScalaBCryptVersion = "4.3.0"
    val AkkaHttpCorsVersion = "1.1.3"
    val JsonXVersion = "0.42.0"
  }

  import Versions._

  object Main {
    val TypeSafeConfig = "com.typesafe" % "config" % TypeSafeConfigVersion
    val AkkaActor = "com.typesafe.akka" %% "akka-actor" % AkkaVersion
    val AkkaHttp = "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
    val AkkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
    val PlayJson = "com.typesafe.play" %% "play-json" % PlayJsonVersion
    val AkkaHttpPlayJson = "de.heikoseeberger" %% "akka-http-play-json" % AkkaHttpPlayJsonVersion
    val Cats = "org.typelevel" %% "cats-core" % CatsVersion
    val MongoDBScalaDriver = "org.mongodb.scala" %% "mongo-scala-driver" % MongoDBScalaDriverVersion
    val ScalaBCrypt = "com.github.t3hnar" %% "scala-bcrypt" % ScalaBCryptVersion
    val HttpCache = "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpVersion
    val AkkaHttpCors =  "ch.megard" %% "akka-http-cors" % AkkaHttpCorsVersion
    val Sendgrid = "com.sendgrid" % "sendgrid-java" % sendgridVersion
    val RollBar = "com.rollbar" % "rollbar-java" % rollbarVersion
    val JsonX = "ai.x" %% "play-json-extensions" % JsonXVersion
    val KeycloakAdapter = "org.keycloak" % "keycloak-adapter-core" % keycloakVersion
    val KeycloakCore = "org.keycloak" % "keycloak-core" % keycloakVersion
    val KeycloakAdmin = "org.keycloak" % "keycloak-admin-client" % keycloakVersion
    val janis =  "org.fusesource.jansi" % "jansi" % "1.12"
    val googleClient = "com.google.api-client" % "google-api-client" % "1.30.9"
    val googleApiService = "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev20191003-1.30.8"
    val googleAuth = "com.google.oauth-client" % "google-oauth-client-jetty" % "1.30.5"
    val googleAuth2 = "com.google.auth" % "google-auth-library-oauth2-http" % "1.3.0"

    val All: Seq[ModuleID] = Seq(
      TypeSafeConfig,
      AkkaActor,
      AkkaHttp,
      AkkaStream,
      PlayJson,
      AkkaHttpPlayJson,
      Cats,
      MongoDBScalaDriver,
      ScalaBCrypt,
      HttpCache,
      AkkaHttpCors,
      Sendgrid,
      KeycloakAdapter,
      KeycloakCore,
      KeycloakAdmin,
      RollBar,
      JsonX,
      janis,
      googleClient,
      googleApiService,
      googleAuth,
      googleAuth2
    )
  }

  object Test {
    val AkkaTestKit = "com.typesafe.akka" % "akka-testkit_2.13" % AkkaVersion
    val AkkaStreamTestKit = "com.typesafe.akka" % "akka-stream-testkit_2.13" % AkkaVersion
    val AkkaHttpTestKit = "com.typesafe.akka" % "akka-http-testkit_2.13" % AkkaHttpVersion
    val ScalaTest = "org.scalatest" % "scalatest_2.13" % ScalaTestVersion
    val MockitoScala = "org.mockito" % "mockito-scala_2.13" % MockitoScalaVersion

    val All: Seq[ModuleID] = Seq(
      AkkaTestKit,
      AkkaHttpTestKit,
      AkkaStreamTestKit,
      ScalaTest,
      MockitoScala
    ).map(_ % Configurations.Test)
  }

}
