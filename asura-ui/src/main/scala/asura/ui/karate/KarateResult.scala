package asura.ui.karate

case class KarateResult(
                         status: String,
                         duration: Long,
                         aborted: Boolean,
                         skipped: Boolean,
                         error: String = null,
                       )

object KarateResult {

  val PASSED = "passed"
  val FAILED = "failed"
  val SKIPPED = "skipped"

  def passed(nanos: Long) = KarateResult(PASSED, nanos, false, false)

  def failed(error: String) = KarateResult(FAILED, 0, false, false, error)

  def skipped() = KarateResult(SKIPPED, 0, false, false)

  def aborted(nanos: Long) = KarateResult(PASSED, nanos, true, false)

}
