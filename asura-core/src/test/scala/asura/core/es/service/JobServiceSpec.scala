package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.Job
import asura.core.es.{EsClient, EsClientConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class JobServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  test("delete-index") {
    EsClient.esClient.execute {
      deleteIndex(Job.Index)
    }.await
  }

  test("create-index") {
    val isOk = IndexService.initCheck(Job)
    assertResult(true)(isOk)
  }
}
