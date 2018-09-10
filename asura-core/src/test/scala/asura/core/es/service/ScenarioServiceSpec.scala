package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.common.util.JsonUtils
import asura.core.cs.model.QueryScenario
import asura.core.es.model.{DocRef, Scenario}
import asura.core.es.{EsClient, EsClientConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class ScenarioServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  private val docRefs = Seq(DocRef("eztxpWMBW6aeaWsYaB8i"), DocRef("fDtxpWMBW6aeaWsYpR-s"), DocRef("fTtxpWMBW6aeaWsYvB9D"))

  test("delete-index") {
    EsClient.esClient.execute {
      deleteIndex(Scenario.Index)
    }.await match {
      case Right(res) =>
        println(res)
      case _ =>
    }
  }

  test("create-index") {
    val isOk = IndexService.initCheck(Scenario)
    assertResult(true)(isOk)
  }

  test("index") {
    val scenario = Scenario(
      summary = "Scenario Test",
      description = "Scenario Test Desc",
      group = "group",
      project = "project",
      cases = docRefs
    )
    val res = ScenarioService.index(scenario).await
    println(res)
  }

  test("update") {
    val json =
      """
        |{
        |"cases": [
        |{
        |"id": "eztxpWMBW6aeaWsYaB8i"
        |},
        |{
        |"id": "fDtxpWMBW6aeaWsYpR-s"
        |},
        |{
        |"id": "fTtxpWMBW6aeaWsYvB9D"
        |}
        |]
        |}
      """.stripMargin
    val res = ScenarioService.updateScenario("fzvMpmMBW6aeaWsYGR-S", JsonUtils.parse(json, classOf[Scenario])).await
    println(res)
  }

  test("query text") {
    val res = ScenarioService.queryScenario(QueryScenario(null, null, "Test")).await
    println(res)
  }

  test("get by ids") {
    val res = ScenarioService
      .getScenariosByIds(Seq("fjvapWMBW6aeaWsYNx_j", "fjvapWMBW6aeaWsYNx_j"))
      .await
    println(res)
  }
}
