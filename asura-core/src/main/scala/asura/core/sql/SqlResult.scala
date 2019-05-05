package asura.core.sql

import asura.core.cs.assertion.engine.{AssertionContext, Statistic}
import asura.core.es.model.SqlRequest

import scala.concurrent.Future

case class SqlResult(
                      val context: Object,
                      var statis: Statistic = Statistic(),
                      var result: java.util.Map[_, _] = java.util.Collections.EMPTY_MAP,
                    ) {

}

object SqlResult {

  def evaluate(request: SqlRequest, context: Object): Future[SqlResult] = {
    import asura.core.concurrent.ExecutionContextManager.sysGlobal
    val statis = Statistic()
    AssertionContext.eval(request.assert, context, statis).map(result => {
      SqlResult(context, statis, result)
    })
  }
}
