import sbt.Keys.{organization, scalaVersion, version}

object BuildSettings {

  lazy val commonSettings = Seq(
    organization := "cc.akkaha",
    version := "0.5.0",
    scalaVersion := "2.12.8"
  )
}
