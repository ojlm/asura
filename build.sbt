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
lazy val root = Project("asura-app", file("."))
  .enablePlugins(PlayScala, SwaggerPlugin)
  .settings(commonSettings: _*)
  .settings(releaseSettings: _*)
  .settings(publishArtifact in Compile := true)
  .dependsOn(
    common % "compile->compile;test->test",
    cluster % "compile->compile;test->test",
    core % "compile->compile;test->test",
    web % "compile->compile;test->test",
    namerd % "compile->compile;test->test",
  ).aggregate(common, cluster, core, web, namerd)


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
  .settings(publishSettings: _*)

lazy val cluster = asuraProjects("asura-cluster")
  .settings(libraryDependencies ++= clusterDependencies)
  .settings(publishSettings: _*)

lazy val core = asuraProjects("asura-core")
  .settings(libraryDependencies ++= coreDependencies)
  .settings(publishSettings: _*)
  .dependsOn(common % "compile->compile;test->test")

lazy val pcap = asuraProjects("asura-pcap")
  .settings(libraryDependencies ++= pcapDependencies)
  .settings(publishSettings: _*)

lazy val web = asuraProjects("asura-web")
  .settings(libraryDependencies ++= webDependencies)
  .settings(publishSettings: _*)
  .dependsOn(common % "compile->compile;test->test")

lazy val namerd = asuraProjects("asura-namerd")
  .settings(libraryDependencies ++= namerdDependencies)
  .settings(publishSettings: _*)
  .dependsOn(common % "compile->compile;test->test")

// release
val username = "asura-pro"
val repo = "indigo-api"

import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

lazy val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    //runClean,
    // runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommand("publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    //pushChanges
  )
)
lazy val publishSettings = Seq(
  homepage := Some(url(s"https://github.com/$username/$repo")),
  licenses += "MIT" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
  apiURL := Some(url(s"https://$username.github.io/$repo/latest/api/")),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  developers := List(
    Developer(
      id = username,
      name = "zhengshaodong",
      email = "ngxcorpio@gmail.com",
      url = new URL(s"http://github.com/${username}")
    )
  ),
  useGpg := true,
  usePgpKeyHex("200BB242B4BE47DD"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
  // Following 2 lines need to get around https://github.com/sbt/sbt/issues/4275
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
)

coverageEnabled := false
