import com.typesafe.sbt.SbtNativePackager.autoImport.maintainer
import sbt.Keys.{organization, scalaVersion, version}

object BuildSettings {

  lazy val commonSettings = Seq(
    organization := "cc.akkaha",
    version := "0.6.0",
    scalaVersion := "2.12.8",
    maintainer := "ngxcorpio@gmail.com"
  )
}
