package asura.core.script.function

import asura.common.util.StringUtils

import scala.concurrent.Future

trait TransformFunction {

  val description = StringUtils.EMPTY
  val name: String

  /** transform function, accept a argument and return one
    *
    */
  def apply(arg: Object): Future[Object]

  override def toString: String = s"func ${name}()"
}
