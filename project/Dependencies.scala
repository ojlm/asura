import play.sbt.PlayImport.{ehcache, filters, guice, ws}
import sbt._

object Dependencies {

  val akkaVersion = "2.6.10"
  val akkaHttpVersion = "10.1.12"

  private val akkaActor = "com.typesafe.akka" %% "akka-actor" % akkaVersion
  private val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  private val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  private val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
  private val akkaJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion
  private val akkaCluster = "com.typesafe.akka" %% "akka-cluster" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaMetrics = "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion excludeAll (ExclusionRule(organization = "io.netty", name = "netty"))
  private val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  private val akkaHttpXml = "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion
  private val config = "com.typesafe" % "config" % "1.4.0"
  private val jackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.3"
  private val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.7"
  private val quartz = "org.quartz-scheduler" % "quartz" % "2.3.0" excludeAll(ExclusionRule(organization = "com.mchange", name = "c3p0"), ExclusionRule(organization = "com.mchange", name = "mchange-commons-java"))
  private val swaggerParser = "io.swagger.parser.v3" % "swagger-parser" % "2.0.20"
  private val elastic4s = "com.sksamuel.elastic4s" %% "elastic4s-http" % "6.7.8"
  private val joddCore = "org.jodd" % "jodd-core" % "3.9.1"
  private val jsonPath = "com.jayway.jsonpath" % "json-path" % "2.5.0"
  private val oshi = "com.github.oshi" % "oshi-core" % "5.8.0"

  val caffeineLib = "com.github.ben-manes.caffeine" % "caffeine" % "2.8.4"
  val kryo = "com.esotericsoftware" % "kryo" % "5.1.1"

  // kafka
  private val akkaKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.5"
  private val kafkaAvroSerializer = "io.confluent" % "kafka-avro-serializer" % "4.0.0" excludeAll (ExclusionRule(organization = "org.slf4j", name = "*"))

  // dubbo, specify javassist and jbossnetty deps because of coursier dep resolve problems
  private val dubbo = "com.alibaba" % "dubbo" % "2.6.5" excludeAll(ExclusionRule(organization = "org.springframework"), ExclusionRule(organization = "org.javassist"), ExclusionRule(organization = "org.jboss.netty"))
  private val dubboJavassist = "org.javassist" % "javassist" % "3.21.0-GA"
  private val dubboJbossNetty = "org.jboss.netty" % "netty" % "3.2.5.Final"
  private val dubboSpring = "org.springframework" % "spring-context" % "4.3.10.RELEASE" % Test
  private val curator = "org.apache.curator" % "curator-recipes" % "2.12.0"

  // Database
  private val mysqlConnector = "mysql" % "mysql-connector-java" % "8.0.20"
  private val redisson = "org.redisson" % "redisson" % "3.5.7"
  private val druid = "com.alibaba" % "druid" % "1.1.16"

  // Log dependencies
  private val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
  private val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  private val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

  // graal js
  val graalvmJsScriptengine = "org.graalvm.js" % "js-scriptengine" % "21.1.0"
  val graalvmJs = "org.graalvm.js" % "js" % "21.1.0"

  // ui
  private val karate = "com.intuit.karate" % "karate-core" % "1.0.1" excludeAll(
    ExclusionRule(organization = "commons-logging", name = "*"),
    ExclusionRule(organization = "info.cukes", name = "*"),
    ExclusionRule(organization = "info.picocli", name = "*"),
  )
  // for syntax highlight in ide
  private val cucumber = "info.cukes" % "cucumber-java" % "1.2.6" % Provided
  private val picocli = "info.picocli" % "picocli" % "4.6.1"
  private val jadb = "com.github.vidstige" % "jadb" % "1.2.1"

  // Test dependencies
  private val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  private val scalaTestDeps = Seq(
    "org.scalactic" %% "scalactic" % "3.1.1",
    "org.scalatest" %% "scalatest" % "3.1.1" % Test
  )
  private val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test

  private val loggingDeps = Seq(slf4jApi, logback, scalaLogging)
  private val httpDeps = Seq(akkaStream, akkaHttp, akkaHttpXml, jackson)
  private val databaseDeps = Seq(mysqlConnector)
  private val testDeps = Seq(akkaTestKit, akkaHttpTestkit) ++ scalaTestDeps

  private val javaCV = Seq(
    "org.bytedeco" % "javacv" % "1.5.4" excludeAll(
      ExclusionRule(organization = "org.bytedeco", name = "flandmark"),
      ExclusionRule(organization = "org.bytedeco", name = "flycapture"),
      ExclusionRule(organization = "org.bytedeco", name = "libdc1394"),
      ExclusionRule(organization = "org.bytedeco", name = "libfreenect"),
      ExclusionRule(organization = "org.bytedeco", name = "libfreenect2"),
      ExclusionRule(organization = "org.bytedeco", name = "librealsense"),
      ExclusionRule(organization = "org.bytedeco", name = "librealsense2"),
      ExclusionRule(organization = "org.bytedeco", name = "videoinput"),
      ExclusionRule(organization = "org.bytedeco", name = "artoolkitplus"),
      ExclusionRule(organization = "org.openjfx", name = "javafx-graphics"),
    ),
    "org.openjfx" % "javafx-graphics" % "16",
    "org.openjfx" % "javafx-base" % "16" classifier "win",
    "org.openjfx" % "javafx-graphics" % "16" classifier "win",
    "org.openjfx" % "javafx-base" % "16" classifier "mac",
    "org.openjfx" % "javafx-graphics" % "16" classifier "mac"
  )

  val luceneVersion = "8.9.0"
  private val lucene = Seq(
    "org.apache.lucene" % "lucene-core" % luceneVersion,
    "org.apache.lucene" % "lucene-analyzers-smartcn" % luceneVersion,
    "org.apache.lucene" % "lucene-queryparser" % luceneVersion,
    "org.apache.lucene" % "lucene-facet" % luceneVersion,
    "org.apache.lucene" % "lucene-highlighter" % luceneVersion
  )

  val commonDependencies = Seq(akkaTestKit, config, akkaActor, jackson, akkaActorTyped, akkaSlf4j, akkaJackson, kryo) ++ scalaTestDeps ++ loggingDeps
  val clusterDependencies = Seq(akkaCluster, akkaMetrics, akkaClusterTools) ++ commonDependencies
  val coreDependencies = Seq(
    commonsLang3, jackson, elastic4s, druid,
    joddCore, jsonPath, swaggerParser, quartz, redisson, graalvmJs, graalvmJsScriptengine
  ) ++ commonDependencies ++ httpDeps ++ databaseDeps
  val appDependencies = Seq(caffeineLib)
  val namerdDependencies = Seq(akkaStream, akkaHttp) ++ commonDependencies
  val dubboDependencies = Seq(dubbo, curator, dubboJavassist, dubboJbossNetty, dubboSpring, akkaStream) ++ commonDependencies
  val kafkaDependencies = Seq(akkaStream, akkaKafka, kafkaAvroSerializer) ++ commonDependencies
  val uiDependencies = Seq(karate, cucumber, picocli, oshi) ++ commonDependencies ++ javaCV
  val uiCliDependencies = Seq(jadb) ++ uiDependencies ++ lucene

  val commonPlayDeps = Seq(
    guice,
    ehcache,
    ws,
    filters,
  )

  val playPac4jVersion = "10.0.0"
  val pac4jVersion = "4.0.0"
  val appPlayDeps = Seq(
    "org.pac4j" %% "play-pac4j" % playPac4jVersion,
    "org.pac4j" % "pac4j-http" % pac4jVersion,
    "org.pac4j" % "pac4j-ldap" % pac4jVersion,
    "org.pac4j" % "pac4j-cas" % pac4jVersion,
    "org.pac4j" % "pac4j-jwt" % pac4jVersion,
    "com.typesafe.play" %% "play-mailer" % "7.0.1",
    "com.typesafe.play" %% "play-mailer-guice" % "7.0.1",
    "com.typesafe.play" %% "play-json" % "2.7.4",
  ) ++ commonPlayDeps
}
