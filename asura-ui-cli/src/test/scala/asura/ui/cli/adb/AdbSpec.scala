package asura.ui.cli.adb

import java.io.{BufferedReader, InputStreamReader}

import se.vidstige.jadb.JadbConnection

object AdbSpec {

  def main(args: Array[String]): Unit = {
    val connection = new JadbConnection()
    val version = connection.getHostVersion
    println(s"version: $version")
    connection.getDevices.forEach(device => {
      val serial = device.getSerial
      println(s"serial: $serial")
      println(s"state: ${device.getState}")
      val stream = device.execute("logcat")
      val reader = new BufferedReader(new InputStreamReader(stream))
      reader.lines().forEach(line => {
        println(s"$serial: $line")
      })
    })
  }

}
