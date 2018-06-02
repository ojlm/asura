package asura.core.cs.asserts

import asura.common.util.LogUtils
import asura.core.cs.operators._
import asura.core.util.JsonPathUtils
import com.jayway.jsonpath.PathNotFoundException
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

object Assert {
  val logger = Logger(classOf[Assert])
}

case class Assert(
                   assert: Map[String, Any],
                   context: Object,
                   statis: Statistic
                 ) {

  import Assert._

  val result = mutable.Map[String, Any]()

  if (null != assert && null != context) {
    for ((k, v) <- assert) {
      if (null == v) {
        statis.failOnce()
        result += (k -> FailAssertResult(1, s"null assert: $assert"))
      } else {
        if (k.startsWith("$.") || k.startsWith("$[")) { // path
          try {
            val subContext = JsonPathUtils.read[Object](context, k)
            val subAssert = Assert(v.asInstanceOf[Map[String, Any]], subContext, statis)
            result += (k -> subAssert.result)
          } catch {
            case _: ClassNotFoundException =>
              statis.failOnce()
              statis.isSuccessful = false
              result += (k -> FailAssertResult(1, AssertResult.MSG_UNSUPPORTED_ASSERT_FORMAT))
            case _: PathNotFoundException =>
              statis.failOnce()
              statis.isSuccessful = false
              result += (k -> FailAssertResult(1, AssertResult.pathNotFound(k)))
            case t: Throwable =>
              statis.failOnce()
              statis.isSuccessful = false
              logger.warn(LogUtils.stackTraceToString(t))
              result += (k -> FailAssertResult(1, t.getMessage))
          }
        } else if (k.startsWith("$")) { // operator
          val assertResult: AssertResult = k match {
            // comparison
            case Operators.EQ =>
              Eq(context, v)
            case Operators.NE =>
              Ne(context, v)
            case Operators.GT =>
              Gt(context, v)
            case Operators.GTE =>
              Gte(context, v)
            case Operators.LT =>
              Lt(context, v)
            case Operators.LTE =>
              Lte(context, v)
            case Operators.IN =>
              In(context, v)
            case Operators.NIN =>
              Nin(context, v)
            case Operators.REGEX =>
              Regex(context, v)
            // logical
            case Operators.AND =>
              And(context, v)
            case Operators.NOT =>
              Not(context, v)
            case Operators.NOR =>
              Nor(context, v)
            case Operators.OR =>
              Or(context, v)
            // element
            case Operators.TYPE =>
              Type(context, v)
            // array
            case Operators.SIZE =>
              Size(context, v)
            // script
            case Operators.SCRIPT =>
              Script(context, v)
            case _ =>
              FailAssertResult(1, AssertResult.MSG_UNSUPPORTED_OPERATOR)
          }
          statis.countAndSetState(assertResult)
          result += (k -> assertResult.toReport)
        } else {
          statis.failOnce()
          result += (k -> FailAssertResult(1, AssertResult.MSG_UNRECOGNIZED_KEY))
        }
      }
    }
  }
}
