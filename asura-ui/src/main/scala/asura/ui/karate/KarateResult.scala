package asura.ui.karate

import asura.common.util.LogUtils
import com.fasterxml.jackson.annotation.JsonIgnore

case class KarateResult(
                         duration: Long = 0,
                         passed: Boolean = false,
                         failed: Boolean = false,
                         aborted: Boolean = false,
                         skipped: Boolean = false,
                         error: String = null,
                         @JsonIgnore var result: Any = null
                       )

object KarateResult {

  def pass(nanos: Long) = KarateResult(duration = nanos, passed = true)

  def pass(nanos: Long, result: Any) = KarateResult(duration = nanos, passed = true, result = result)

  def fail(error: String) = KarateResult(failed = true, error = error)

  def fail(error: String, nanos: Long) = KarateResult(duration = nanos, failed = true, error = error)

  def fail(error: Throwable, nanos: Long) = {
    KarateResult(duration = nanos, failed = true, error = LogUtils.stackTraceToString(error))
  }

  def skip() = KarateResult(skipped = true)

  def abort(nanos: Long) = KarateResult(duration = nanos, aborted = true)

}
