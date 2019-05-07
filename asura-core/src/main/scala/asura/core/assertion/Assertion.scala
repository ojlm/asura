package asura.core.assertion

import asura.common.util.StringUtils
import asura.core.assertion.engine.AssertResult

import scala.concurrent.Future

trait Assertion {

  val description = StringUtils.EMPTY
  val name: String

  /**
    *
    * @param actual the actual value from context
    * @param expect the expect value
    * @return
    */
  def assert(actual: Any, expect: Any): Future[AssertResult]
}
