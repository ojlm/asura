package asura.core.cs

import asura.core.cs.asserts.{Assert, Statistic}
import asura.core.es.model.Case

case class CaseResult(
                       var id: String,
                       var assert: Map[String, Any],
                       var context: java.util.Map[Any, Any],
                       var request: CaseRequest,
                     ) {

  val statis = Statistic()
  var result = Assert(assert, context, statis).result
}

object CaseResult {

  def failResult(id: String, cs: Case): CaseResult = {
    val result = CaseResult(id, cs.assert, null, null)
    result.statis.isSuccessful = false
    result
  }
}
