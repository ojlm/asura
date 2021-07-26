package asura.common.util

object ResourceUtils {

  def getAsBytes(path: String): Array[Byte] = {
    FileUtils.toBytes(getClass.getClassLoader.getResourceAsStream(path))
  }

}
