package asura.ui.cli.utils

import java.io._

import asura.ui.cli.runner.AndroidRunner.AppOptions
import asura.ui.cli.server.HttpPageHandler
import com.typesafe.scalalogging.Logger
import se.vidstige.jadb.{JadbDevice, RemoteFile}

object AdbDeviceUtils {

  val logger = Logger(AdbDeviceUtils.getClass)
  val DATA_LOCAL_PATH = "/data/local/tmp"
  val STATIC_APK_FILE_NAME = "asura-ui.apk"
  val STATIC_APK_FILE_PATH = s"static/$STATIC_APK_FILE_NAME"
  val APP_NAME = "asura.ui.app"

  def prepareDevice(device: JadbDevice, file: File = null): Unit = {
    val inputStream = if (file != null) {
      new FileInputStream(file)
    } else {
      HttpPageHandler.getSteamFromStaticResource(STATIC_APK_FILE_PATH)
    }
    install(device, inputStream, STATIC_APK_FILE_NAME)
    disableBatteryOptimization(device, APP_NAME)
  }

  def runApp(device: JadbDevice, options: AppOptions, online: String => Unit = null): Unit = {
    val command = s"CLASSPATH=$DATA_LOCAL_PATH/$STATIC_APK_FILE_NAME " +
      s"app_process / asura.ui.app.Server ${options.toOptions()}"
    val stream = device.execute(command)
    val reader = new BufferedReader(new InputStreamReader(stream))
    reader.lines().forEach(line => {
      if (online != null) {
        online(line)
      } else {
        logger.info(s"${device.getSerial}: $line")
      }
    })
  }

  def disableBatteryOptimization(device: JadbDevice, name: String): String = {
    val ret = new String(device.execute("dumpsys", "deviceidle", "whitelist", s"+$name").readAllBytes())
    if (!ret.startsWith("Added")) throw new RuntimeException(ret)
    ret
  }

  def install(device: JadbDevice, name: String): String = {
    val ret = new String(device.execute("pm", "install", name).readAllBytes())
    if (!ret.startsWith("Success")) throw new RuntimeException(ret)
    ret
  }

  def pushApp(device: JadbDevice, source: File) = {
    device.push(source, new RemoteFile(s"$DATA_LOCAL_PATH/${source.getName}"))
  }

  def pushApp(device: JadbDevice, source: InputStream, name: String) = {
    device.push(source, System.currentTimeMillis(), 0x1b4 /*0664*/ , new RemoteFile(s"$DATA_LOCAL_PATH/$name"))
  }

  def install(device: JadbDevice, source: InputStream, name: String): String = {
    pushApp(device, source, name)
    install(device, s"$DATA_LOCAL_PATH/$name")
  }

}
