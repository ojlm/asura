package asura.core.cs

import asura.core.cs.asserts.{Assert, Statistic}
import asura.core.es.model.Case

case class CaseResult(
                       id: String,
                       assert: Map[String, Any],
                       context: java.util.Map[Any, Any],
                       request: CaseRequest,
                     ) {
  val statis = Statistic()
  val result = Assert(assert, context, statis).result
}

object CaseResult {

  def failResult(id: String, cs: Case): CaseResult = {
    val result = CaseResult(id, cs.assert, null, null)
    result.statis.isSuccessful = false
    result
  }
}
