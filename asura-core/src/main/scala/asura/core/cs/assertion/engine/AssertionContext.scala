package asura.core.cs.assertion.engine

import asura.common.util.LogUtils
import asura.core.concurrent.ExecutionContextManager.cachedExecutor
import asura.core.cs.assertion._
import asura.core.util.JsonPathUtils
import com.jayway.jsonpath.PathNotFoundException
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future

case class AssertionContext(
                             assert: Map[String, Any],
                             context: Object,
                             statis: Statistic
                           )

object AssertionContext {

  val logger = Logger(classOf[AssertionContext])

  def eval(assert: Map[String, Any], context: Object, statis: Statistic): Future[java.util.Map[String, Any]] = {
    val result = new java.util.concurrent.ConcurrentHashMap[String, Any]()
    if (null != assert && null != context) {
      val resultFutures = for ((k, v) <- assert) yield {
        if (null == v) {
          statis.failOnce()
          val assertionResult = FailAssertResult(1, s"null assert: $assert")
          result.put(k, assertionResult)
          Future.successful(result)
        } else {
          if (k.startsWith("$.") || k.startsWith("$[")) { // path
            handleJsonPath(context, k, v, result, statis)
          } else if (k.startsWith("$")) { // assertion
            val assertion = Assertions.get(k)
            if (assertion.nonEmpty) {
              assertion.get.assert(context, v).map(assertionResult => {
                if (null != assertionResult) {
                  statis.countAndSetState(assertionResult)
                  result.put(k, assertionResult.toReport)
                }
                result
              })
            } else {
              result.put(k, FailAssertResult(1, AssertResult.MSG_UNSUPPORTED_ASSERTION))
              Future.successful(result)
            }
          } else {
            statis.failOnce()
            result.put(k, FailAssertResult(1, AssertResult.MSG_UNRECOGNIZED_KEY))
            Future.successful(result)
          }
        }
      }
      Future.sequence(resultFutures).map(_ => result)
    } else {
      Future.successful(result)
    }
  }

  private def handleJsonPath(
                              context: Object,
                              k: String,
                              v: Any,
                              result: java.util.Map[String, Any],
                              statis: Statistic
                            ): Future[java.util.Map[String, Any]] = {
    try {
      val subContext = JsonPathUtils.read[Object](context, k)
      AssertionContext.eval(v.asInstanceOf[Map[String, Any]], subContext, statis).map { subAssert =>
        result.put(k, subAssert)
        result
      }.recover {
        case _: ClassNotFoundException =>
          statis.failOnce()
          statis.isSuccessful = false
          result.put(k, FailAssertResult(1, AssertResult.MSG_UNSUPPORTED_ASSERT_FORMAT))
          result
        case t: Throwable =>
          statis.failOnce()
          statis.isSuccessful = false
          logger.warn(LogUtils.stackTraceToString(t))
          result.put(k, FailAssertResult(1, t.getMessage))
          result
      }
    } catch {
      case _: PathNotFoundException =>
        statis.failOnce()
        statis.isSuccessful = false
        result.put(k, FailAssertResult(1, AssertResult.pathNotFound(k)))
        Future.successful(result)
      case t: Throwable =>
        statis.failOnce()
        statis.isSuccessful = false
        logger.warn(LogUtils.stackTraceToString(t))
        result.put(k, FailAssertResult(1, t.getMessage))
        Future.successful(result)
    }
  }
}
