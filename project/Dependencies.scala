import play.sbt.PlayImport.{ehcache, filters, guice, ws}
import sbt._

object Dependencies {

  val akkaVersion = "2.5.20"
  val akkaHttpVersion = "10.1.7"
  val elastic4sVersion = "6.3.7"
  val playPac4jVersion = "6.0.0"
  val pac4jVersion = "3.0.1"

  private val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  private val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  private val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaMetrics = "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  private val akkaHttpXml = "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion
  private val config = "com.typesafe" % "config" % "1.3.2"
  private val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.1"
  private val guava = "com.google.guava" % "guava" % "23.6-jre"
  private val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
  private val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.7"
  private val quartz = "org.quartz-scheduler" % "quartz" % "2.3.0" excludeAll(ExclusionRule(organization = "com.mchange", name = "c3p0"), ExclusionRule(organization = "com.mchange", name = "mchange-commons-java"))
  private val swaggerParser = "io.swagger" % "swagger-parser" % "1.0.33"
  private val faststring = "com.dongxiguo" %% "fastring" % "0.3.1"
  private val elastic4sCore = "com.sksamuel.elastic4s" %% "elastic4s-core" % elastic4sVersion
  private val elastic4sHttp = "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion
  private val elastic4sEmbedded = "com.sksamuel.elastic4s" %% "elastic4s-embedded" % elastic4sVersion excludeAll (ExclusionRule(organization = "org.apache.logging.log4j", name = "log4j-slf4j-impl"))
  private val joddCore = "org.jodd" % "jodd-core" % "3.9.1"
  private val jsonPath = "com.jayway.jsonpath" % "json-path" % "2.4.0"
  private val gatling = "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.3"

  // dubbo, specify javassist and jbossnetty deps because of coursier dep resolve problems
  private val dubbo = "com.alibaba" % "dubbo" % "2.6.5" excludeAll(ExclusionRule(organization = "org.springframework"), ExclusionRule(organization = "org.javassist"), ExclusionRule(organization = "org.jboss.netty"))
  private val dubboJavassist = "org.javassist" % "javassist" % "3.21.0-GA"
  private val dubboJbossNetty = "org.jboss.netty" % "netty" % "3.2.5.Final"
  private val dubboSpring = "org.springframework" % "spring-context" % "4.3.10.RELEASE" % Test
  private val curator = "org.apache.curator" % "curator-recipes" % "2.12.0"

  // Database
  private val mysqlConnector = "mysql" % "mysql-connector-java" % "5.1.23"
  private val redisson = "org.redisson" % "redisson" % "3.5.7"
  private val druid = "com.alibaba" % "druid" % "1.1.16"

  // Log dependencies
  private val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
  private val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  private val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.0"

  // Test dependencies
  private val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  private val scalaTestDeps = Seq(
    "org.scalactic" %% "scalactic" % "3.0.5",
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
  private val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test

  private val loggingDeps = Seq(slf4jApi, logback, scalaLogging)
  private val httpDeps = Seq(akkaStream, akkaHttp, akkaHttpXml, jackson)
  private val databaseDeps = Seq(mysqlConnector)
  private val testDeps = Seq(akkaTestKit, akkaHttpTestkit) ++ scalaTestDeps

  val pac4jDeps = Seq(
    "org.pac4j" %% "play-pac4j" % playPac4jVersion,
    "org.pac4j" % "pac4j-ldap" % pac4jVersion
  )

  val commonDependencies = Seq(akkaTestKit, config, akkaActor, jackson) ++ scalaTestDeps ++ loggingDeps
  val clusterDependencies = Seq(akkaCluster, akkaMetrics, akkaClusterTools) ++ commonDependencies
  val coreDependencies = Seq(
    commonsLang3, jackson, faststring, elastic4sCore, elastic4sHttp, druid,
    elastic4sEmbedded, joddCore, jsonPath, swaggerParser, quartz, redisson
  ) ++ commonDependencies ++ httpDeps ++ databaseDeps
  val appDependencies = Seq(config, guava, commonsCodec) ++ loggingDeps ++ httpDeps ++ testDeps
  val namerdDependencies = Seq(akkaStream, akkaHttp) ++ commonDependencies
  val dubboDependencies = Seq(dubbo, curator, dubboJavassist, dubboJbossNetty, dubboSpring, akkaStream) ++ commonDependencies
  val gatlingDependencies = Seq(gatling) ++ clusterDependencies

  val commonPlayDeps = Seq(
    guice,
    ehcache,
    ws,
    filters,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  )

  val appPlayDeps = Seq(
    "org.webjars" % "swagger-ui" % "3.17.4",
    "org.webjars.npm" % "swagger-editor-dist" % "3.1.16",
    "org.pac4j" %% "play-pac4j" % "6.0.0",
    "org.pac4j" % "pac4j-http" % "3.0.1",
    "org.pac4j" % "pac4j-ldap" % "3.0.1",
    "org.pac4j" % "pac4j-jwt" % "3.0.1",
    "com.typesafe.play" %% "play-mailer" % "6.0.1",
    "com.typesafe.play" %% "play-mailer-guice" % "6.0.1",
    "com.typesafe.play" %% "play-json" % "2.7.1",
  ) ++ commonPlayDeps
}
