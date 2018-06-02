package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.Case
import asura.core.es.{EsClient, EsClientConfig, EsConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class CaseServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  test("delete-index") {
    EsClient.httpClient.execute {
      deleteIndex(Case.Index)
    }.await match {
      case Right(res) =>
        println(res)
      case _ =>
    }
  }

  test("create-index") {
    val isOk = IndexService.initCheck(Case)
    assertResult(true)(isOk)
  }

  test("update-assert-by-script") {
    EsClient.httpClient.execute {
      update("yeXvo2IBW6aeaWsYP5dr").in(Case.Index / EsConfig.DefaultType).script {
        script("ctx._source.assert = params.assert").params(Map("assert" -> Map("aaa" -> "aaaa")))
      }
    }.await match {
      case Right(res) =>
        println(res)
      case Left(t) =>
        println(t)
    }
  }
}
