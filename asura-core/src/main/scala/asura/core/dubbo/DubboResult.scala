package asura.core.dubbo

import asura.core.cs.assertion.engine.{AssertionContext, Statistic}
import asura.core.es.model.DubboRequest

import scala.concurrent.Future

case class DubboResult(
                        val context: Object,
                        var statis: Statistic = Statistic(),
                        var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP,
                      ) {

}

object DubboResult {

  def evaluate(request: DubboRequest, context: Object): Future[DubboResult] = {
    import asura.core.concurrent.ExecutionContextManager.sysGlobal
    val statis = Statistic()
    AssertionContext.eval(request.assert, context, statis).map(result => {
      DubboResult(context, statis, result)
    })
  }
}
