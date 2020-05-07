package asura.core.assertion.engine

/** single operator result */
case class AssertReport(passed: Boolean, msg: String)

object AssertResult {

  val MSG_PASSED = "pass"
  val MSG_FAILED = "fail"
  val MSG_INTERNAL_ERROR = "Internal error"
  val MSG_UNRECOGNIZED_KEY = "Unrecognized key"
  val MSG_UNRECOGNIZED_TYPE = "Unrecognized type"
  val MSG_UNSUPPORTED_ASSERTION = "Unsupported assertion"
  val MSG_UNSUPPORTED_TYPE = "Unsupported type"
  val MSG_PATH_NOT_FOUND = "Path not found"
  val MSG_INCOMPARABLE = "Incomparable type"
  val MSG_UNSUPPORTED_ASSERT_FORMAT = "Unsupported assert format"

  val KEY_REPORT_PASS = "passed"
  val KEY_REPORT_MSG = "msg"

  def msgIncomparableSourceType(src: Any): String = {
    "$MSG_INCOMPARABLE, src:${src.getClass.getName}"
  }

  def msgIncomparableTargetType(target: Any): String = {
    "$MSG_INCOMPARABLE, target:${target.getClass.getName}"
  }

  def msgIncomparableType(src: Any, target: Any): String = {
    "$MSG_INCOMPARABLE, src:${src.getClass.getName}, target:${target.getClass.getName}"
  }

  def pathNotFound(path: String) = "$MSG_PATH_NOT_FOUND: $path"

  def msgNotSameType(src: Any, target: Any): String = {
    "Incompatible type, src:${src.getClass.getName}, target:${target.getClass.getName}"
  }
}

/** single operator or composite operator result used internally  */
case class AssertResult(
                         var passed: Int = 0,
                         var failed: Int = 0,
                         var isSuccessful: Boolean = false,
                         var msg: String = AssertResult.MSG_INTERNAL_ERROR,
                         var subResult: Any = null
                       ) {

  /** if `subResults` is not null, this is composite `AssertResult` */
  def toReport: Any = {
    if (null == subResult) {
      AssertReport(isSuccessful, msg)
    } else {
      subResult
    }
  }

  def pass(count: Int): Unit = {
    passed = passed + count
  }

  def passOnce(): Unit = {
    passed = passed + 1
  }

  def fail(count: Int): Unit = {
    failed = failed + count
  }

  def failOnce(): Unit = {
    failed = failed + 1
  }
}

object PassAssertResult {
  def apply(passed: Int = 1): AssertResult = {
    AssertResult(passed = passed, failed = 0, isSuccessful = true, msg = AssertResult.MSG_PASSED)
  }
}

object FailAssertResult {
  def apply(failed: Int = 1, msg: String = AssertResult.MSG_FAILED): AssertResult = {
    AssertResult(passed = 0, failed = failed, isSuccessful = false, msg = msg)
  }
}
