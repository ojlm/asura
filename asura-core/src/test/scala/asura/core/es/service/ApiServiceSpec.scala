package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.RestApi
import asura.core.es.{EsClient, EsClientConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class ApiServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  test("delete-index") {
    EsClient.httpClient.execute {
      deleteIndex(RestApi.Index)
    }.await match {
      case Right(res) =>
        println(res)
      case _ =>
    }
  }

  test("create-index") {
    val isOk = IndexService.initCheck(RestApi)
    assertResult(true)(isOk)
  }

  test("bulk-index") {
    val apis = Seq(
      RestApi(group = "test", path = "/a", method = "a", project = "test"),
      RestApi(group = "test", path = "/b", method = "b", project = "test")
    )
    ApiService.index(apis).await match {
      case Right(res) =>
        println(s"right: $res")
      case Left(failure) =>
        println(failure.error.reason)
    }
  }
}
