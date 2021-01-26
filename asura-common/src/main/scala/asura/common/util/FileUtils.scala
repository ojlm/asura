package asura.common.util

import java.io._
import java.nio.charset.StandardCharsets

object FileUtils {

  def toByteStream(is: InputStream): ByteArrayOutputStream = {
    val result = new ByteArrayOutputStream()
    val buffer = new Array[Byte](1024)
    var length = 0
    try {
      while (length != -1) {
        length = is.read(buffer)
        if (length != -1) result.write(buffer, 0, length)
      }
      result
    } catch {
      case e: IOException => throw new RuntimeException(e)
    }
  }

  def toString(is: InputStream): String = {
    try {
      toByteStream(is).toString(StandardCharsets.UTF_8)
    } catch {
      case e: Exception => throw new RuntimeException(e)
    }
  }

  def toString(file: File): String = {
    try {
      toString(new FileInputStream(file))
    } catch {
      case e: FileNotFoundException => throw new RuntimeException(e)
    }
  }

  def getFileExtension(name: String): String = {
    if (StringUtils.isNotEmpty(name)) {
      val idx = name.lastIndexOf('.')
      if (idx > -1 && idx != name.length - 1) {
        name.substring(idx + 1)
      } else {
        null
      }
    } else {
      null
    }
  }

}
