package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.common.util.JsonUtils
import asura.core.cs.model.QueryScenario
import asura.core.es.model.Scenario
import asura.core.es.{EsClient, EsClientConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class ScenarioServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  //  test("delete-index") {
  //    EsClient.esClient.execute {
  //      deleteIndex(Scenario.Index)
  //    }
  //  }
  //
  //  test("create-index") {
  //    val isOk = IndexService.initCheck(Scenario)
  //    assertResult(true)(isOk)
  //  }
  //
  //  test("update") {
  //    val json =
  //      """
  //        |{
  //        |"cases": [
  //        |{
  //        |"id": "eztxpWMBW6aeaWsYaB8i"
  //        |},
  //        |{
  //        |"id": "fDtxpWMBW6aeaWsYpR-s"
  //        |},
  //        |{
  //        |"id": "fTtxpWMBW6aeaWsYvB9D"
  //        |}
  //        |]
  //        |}
  //      """.stripMargin
  //    ScenarioService.updateScenario("fzvMpmMBW6aeaWsYGR-S", JsonUtils.parse(json, classOf[Scenario])).await
  //  }
  //
  //  test("query text") {
  //    ScenarioService.queryScenario(QueryScenario(null, null, "Test", Nil)).await
  //  }
  //
  //  test("get by ids") {
  //    ScenarioService.getScenariosByIds(Seq("fjvapWMBW6aeaWsYNx_j", "fjvapWMBW6aeaWsYNx_j")).await
  //  }
}
