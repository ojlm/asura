package asura.core.es

import asura.common.ScalaTestBaseSpec
import asura.core.es.model._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl.{deleteIndex, _}
import com.sksamuel.elastic4s.http.HttpClient

class EsClientSpec extends ScalaTestBaseSpec with EsClientConfig {

  //  test("get-http-client") {
  //    println(EsClient.esClient.client)
  //  }
  //
  //  test("del-all-indices") {
  //    val indices = Seq(Case, RestApi, Job, Project, Environment, Group, JobReport, Scenario)
  //    for (index <- indices) {
  //      EsClient.esClient.execute(deleteIndex(index.Index)).await
  //    }
  //  }
}
