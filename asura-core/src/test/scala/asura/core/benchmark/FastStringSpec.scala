package asura.core.benchmark

import asura.common.ScalaTestBaseSpec
import com.dongxiguo.fastring.Fastring.Implicits._

class FastStringSpec extends ScalaTestBaseSpec {

  test("fast-benchmark") {
    val count = 1000000

    val alpha = "alpha"
    val beta = "beta"
    val gamma = "gamma"

    var start = System.nanoTime()
    for (i <- 1 to count) {
      fast"$alpha:$beta$gamma".toString.length > 0
    }
    println(s"fast: ${(System.nanoTime() - start) / count} ns")

    start = System.nanoTime()
    for (i <- 1 to count) {
      s"$alpha:$beta$gamma".length > 0
    }
    println(s"s: ${(System.nanoTime() - start) / count} ns")

    start = System.nanoTime()
    for (i <- 1 to count) {
      val sb = StringBuilder.newBuilder
      sb.append(alpha).append(beta).append(gamma).toString().length > 0
    }
    println(s"sb: ${(System.nanoTime() - start) / count} ns")
  }
}
