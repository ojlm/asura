package asura.ui.util

import java.util.concurrent.ThreadLocalRandom

import scala.util.Random

object RandomStringUtils {

  val RANDOM = new Random(ThreadLocalRandom.current())

  def nextString(len1: Int, len2: Int, cjkRatio: Float): String = {
    if (len1 < 0 || len1 > len2) "" else nextString(RANDOM.nextInt(len2 - len1 + 1) + len1, cjkRatio)
  }

  def nextString(length: Int, cjkRatio: Float): String = {
    if (length <= 0) {
      ""
    } else {
      val arr = new Array[Char](length)
      var i = 0
      while (i < length) {
        arr(i) = if (RANDOM.nextFloat() <= cjkRatio) {
          nextChineseChar()
        } else {
          nextPrintableChar()
        }
        i += 1
      }
      new String(arr)
    }
  }

  def nextPrintableChar(): Char = {
    RANDOM.nextPrintableChar()
  }

  def nextChineseChar(): Char = {
    ((0x4e00 + (RANDOM.nextDouble() * (0x9fa5 - 0x4e00 + 1)).toInt).toChar)
  }

}
