package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.Environment
import asura.core.es.{EsClient, EsClientConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class EnvironmentServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  //  test("delete-index") {
  //    EsClient.esClient.execute {
  //      deleteIndex(Environment.Index)
  //    }.await
  //  }
  //
  //  test("create-index") {
  //    val isOk = IndexService.initCheck(Environment)
  //    assertResult(true)(isOk)
  //  }
}
