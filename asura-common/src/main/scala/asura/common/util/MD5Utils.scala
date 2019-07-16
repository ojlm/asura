package asura.common.util

import java.security.MessageDigest

object MD5Utils {

  val HEX_ARRAY = "0123456789abcdef".toCharArray()

  def encodeToHex(plainText: String): String = {
    val md5Bytes = MessageDigest.getInstance("MD5").digest(plainText.getBytes)
    bytesToHex(md5Bytes)
  }

  def bytesToHex(bytes: Array[Byte]): String = {
    val hexChars = new Array[Char](bytes.length * 2)
    for (i <- 0 until bytes.length) {
      val v = bytes(i) & 0xFF
      hexChars(i * 2) = HEX_ARRAY(v >>> 4)
      hexChars(i * 2 + 1) = HEX_ARRAY(v & 0x0F)
    }
    new String(hexChars)
  }
}
