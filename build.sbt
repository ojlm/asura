import BuildSettings._
import Dependencies._
import sbt.Keys._
import sbt._

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation", "-language:postfixOps", "-language:higherKinds", "-language:implicitConversions")

// docker
packageName in Docker := "asura"
version in Docker := "0.0.0"

// Root
lazy val root = Project("asura", file("."))
  .enablePlugins(PlayScala)
  .dependsOn(app, common, web, core, namerd)
  .settings(commonSettings: _*)
  .settings(publishArtifact in Compile := true)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

// Sub Projects
def asuraProjects(id: String) = Project(id, file(id))
  .settings(commonSettings: _*)

lazy val app = asuraProjects("asura-app")
  .settings(libraryDependencies ++= appDependencies)
  .dependsOn(common, web, core, namerd)
  .settings(mainClass in Compile := Some("asura.app.Asura"))
  .enablePlugins(JavaAppPackaging, JavaServerAppPackaging, DockerPlugin)

lazy val common = asuraProjects("asura-common")
  .settings(libraryDependencies ++= commonDependencies)

lazy val core = asuraProjects("asura-core")
  .settings(libraryDependencies ++= coreDependencies)
  .dependsOn(common)

lazy val pcap = asuraProjects("asura-pcap")
  .settings(libraryDependencies ++= pcapDependencies)

lazy val web = asuraProjects("asura-web")
  .settings(libraryDependencies ++= webDependencies)
  .dependsOn(common)

lazy val namerd = asuraProjects("asura-namerd")
  .settings(libraryDependencies ++= namerdDependencies)
  .dependsOn(common)
