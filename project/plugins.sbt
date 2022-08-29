addSbtPlugin("com.frugalmechanic" % "fm-sbt-s3-resolver" % "0.14.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")

addSbtPlugin("com.github.tkawachi" % "sbt-repeat" % "0.1.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.0.0")

addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.1.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

addSbtPlugin("com.github.mwz" % "sbt-sonar" % "2.1.1")

//sbt plugin to load environment variables from .env into the JVM System Environment for local development.
addSbtPlugin("au.com.onegeek" %% "sbt-dotenv" % "2.1.146")
