package asura.core.assertion.engine

/**
  * this class is not thread safe.
  * passed and failed count are actually checked count may not the same with the assert count
  **/
case class Statistic(
                      var passed: Int = 0,
                      var failed: Int = 0,
                    ) {
  var isSuccessful = true

  def total = passed + failed

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

  def countAndSetState(assertResult: AssertResult): Unit = {
    if (!assertResult.isSuccessful) {
      isSuccessful = false
    }
    pass(assertResult.passed)
    fail(assertResult.failed)
  }
}
