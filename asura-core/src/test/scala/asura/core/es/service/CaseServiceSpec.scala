package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.HttpCaseRequest
import asura.core.es.{EsClient, EsClientConfig, EsConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class CaseServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  //  test("delete-index") {
  //    EsClient.esClient.execute {
  //      deleteIndex(Case.Index)
  //    }.await
  //  }
  //
  //  test("create-index") {
  //    val isOk = IndexService.initCheck(Case)
  //    assertResult(true)(isOk)
  //  }
  //
  //  test("update-assert-by-script") {
  //    EsClient.esClient.execute {
  //      update("yeXvo2IBW6aeaWsYP5dr").in(Case.Index / EsConfig.DefaultType).script {
  //        script("ctx._source.assert = params.assert").params(Map("assert" -> Map("aaa" -> "aaaa")))
  //      }
  //    }.await
  //  }
}
