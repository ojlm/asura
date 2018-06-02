package asura.core.es.service

import asura.common.ScalaTestBaseSpec
import asura.core.es.model.Group
import asura.core.es.{EsClient, EsClientConfig}
import com.sksamuel.elastic4s.http.ElasticDsl._

class GroupServiceSpec extends ScalaTestBaseSpec with EsClientConfig {

  test("delete-group") {
    EsClient.httpClient.execute {
      deleteIndex(Group.Index)
    }.await match {
      case Right(res) =>
        println(res)
      case _ =>
    }
  }

  test("create-index") {
    val isOk = IndexService.initCheck(Group)
    assertResult(true)(isOk)
  }

  test("index-group") {
    val g = Group(
      id = "mr.t",
      summary = "",
      description = ""
    )
    GroupService.index(g).await match {
      case Right(success) =>
        println(success.result)
      case Left(_) =>
    }
  }
}
