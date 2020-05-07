import com.typesafe.sbt.SbtNativePackager.autoImport.maintainer
import sbt.Keys.{organization, scalaVersion, version}

object BuildSettings {

  lazy val commonSettings = Seq(
    organization := "cc.akkaha",
    //scalaVersion := "2.13.2",
    version := "0.7.0",
    maintainer := "ngxcorpio@gmail.com"
  )
}
