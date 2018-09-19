import BuildSettings._
import Dependencies._
import sbt.Keys._
import sbt._

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation", "-language:postfixOps", "-language:higherKinds", "-language:implicitConversions")

// docker
packageName in Docker := "asura"
version in Docker := "0.0.0"

// swagger
swaggerDomainNameSpaces := Seq("asura.app.api.model", "asura.core.es.model")
swaggerV3 := true

// Root
lazy val root = Project("asura", file("."))
  .enablePlugins(PlayScala, SwaggerPlugin)
  .dependsOn(core, web, namerd)
  .settings(commonSettings: _*)
  .settings(publishArtifact in Compile := true)

libraryDependencies ++= Seq(
  guice,
  ehcache,
  ws,
  filters,
  "org.webjars" % "swagger-ui" % "3.17.4",
  "org.webjars.npm" % "swagger-editor-dist" % "3.1.16",
  "org.pac4j" %% "play-pac4j" % "6.0.0",
  "org.pac4j" % "pac4j-http" % "3.0.1",
  "org.pac4j" % "pac4j-ldap" % "3.0.1",
  "org.pac4j" % "pac4j-jwt" % "3.0.1",
  "com.typesafe.play" %% "play-mailer" % "6.0.1",
  "com.typesafe.play" %% "play-mailer-guice" % "6.0.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

// Sub Projects
def asuraProjects(id: String) = Project(id, file(id))
  .settings(commonSettings: _*)

lazy val common = asuraProjects("asura-common")
  .settings(libraryDependencies ++= commonDependencies)

lazy val core = asuraProjects("asura-core")
  .settings(libraryDependencies ++= coreDependencies)
  .dependsOn(common % "compile->compile;test->test")

lazy val pcap = asuraProjects("asura-pcap")
  .settings(libraryDependencies ++= pcapDependencies)

lazy val web = asuraProjects("asura-web")
  .settings(libraryDependencies ++= webDependencies)
  .dependsOn(common % "compile->compile;test->test")

lazy val namerd = asuraProjects("asura-namerd")
  .settings(libraryDependencies ++= namerdDependencies)
  .dependsOn(common % "compile->compile;test->test")
