package asura.common.util

import java.security.MessageDigest

object MD5Utils {

  def encode(plainText: String): String = {
    val md5Bytes = MessageDigest.getInstance("MD5").digest(plainText.getBytes)
    val sb = new StringBuilder(BigInt(1, md5Bytes).toString(16))
    sb.toString
  }
}
