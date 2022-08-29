import Dependencies.Versions
import sbt.Test
import java.util.Properties

ThisBuild / scapegoatVersion := "1.4.13"
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

dockerBaseImage       := "openjdk:jre"
dockerExposedPorts := Seq(8000)

val appProperties = settingKey[Properties]("The application properties")

name := """knolx-image"""
version := "1.0"

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("src/main/resources/application.conf"))
  prop
}

Compile / mainClass := Some("com.knoldus.HttpServerApp")

lazy val KnowledgePortalRest = Project("knowledge-portal-rest", file(".")).settings(
  name := "knowledge-portal-rest",
  organization := "Knoldus",
  description := "Knowledge Portal REST API",
  version := appProperties.value.getProperty("api-version"),
  homepage := Some(url("http://knolx.knoldus.com")),
  coverageMinimum := 84.50,
  coverageFailOnMinimum := true,
  coverageExcludedPackages := "<empty>;dao.*",
  Compile / mainClass := Some("com.knoldus.HttpServerApp"),
  scalaVersion := Versions.ScalaVersion,
  scalacOptions ++= Seq(
        s"-target:jvm-${Versions.JDKVersion}",
        "-encoding",
        "UTF-8",
        "-Xlint:-unused,_",
        "-Ywarn-dead-code",
        "-Ywarn-unused:imports",
        "-Ywarn-unused:locals",
        "-Ywarn-unused:patvars",
        "-Ywarn-unused:privates",
        "-deprecation"
      ),
  Test / scalacOptions -= "-Ywarn-dead-code",
  javacOptions ++= Seq(
        "-source",
        Versions.JDKVersion,
        "-target",
        Versions.JDKVersion,
        "-encoding",
        "UTF-8"
      ),
  libraryDependencies ++= Dependencies.Main.All ++ Dependencies.Test.All,
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oD"),
    sonarProperties := Map(
    "sonar.host.url" -> "https://sonar.knoldus.com",
    "sonar.login" -> "5b965b738afdda03cff686ea04ea3b5fbe3c929d",
    "sonar.projectName" -> "Knolx Portal REST API",
    "sonar.projectKey" -> "knolx-portal-rest",
    "sonar.language" -> "scala",
    "sonar.sources" -> "src/main/scala",
    "sonar.tests" -> "src/test/scala",
    "sonar.scala.scalastyle.reportPaths" -> "target/scalastyle-result.xml",
    "sonar.scala.coverage.reportPaths" -> "target/scala-2.13/scoverage-report/scoverage.xml",
    "sonar.scala.scapegoat.reportPaths" -> "target/scala-2.13/scapegoat-report/scapegoat-scalastyle.xml"
  )

)

assembly / assemblyMergeStrategy := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
