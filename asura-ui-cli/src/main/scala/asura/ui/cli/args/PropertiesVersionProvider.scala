package asura.ui.cli.args

import java.util.Properties

import picocli.CommandLine

class PropertiesVersionProvider extends CommandLine.IVersionProvider {
  @throws[Exception]
  override def getVersion: Array[String] = {
    val url = getClass.getResource("/version.txt")
    if (url == null) {
      Array("No version.txt file found in the classpath.")
    } else {
      val properties = new Properties()
      properties.load(url.openStream)
      Array(
        s"${properties.getProperty("name")} ${properties.getProperty("version")}",
      )
    }
  }
}
