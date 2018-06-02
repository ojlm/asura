package asura.core.es

import asura.core.es.model._
import asura.core.es.service.IndexService
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.scalalogging.Logger

object EsClient {

  val logger = Logger("EsClient")
  private var client: HttpClient = null

  def httpClient: HttpClient = client

  /**
    * check if index exists, if not create
    */
  def init(url: String): Boolean = {
    client = HttpClient(ElasticsearchClientUri(url))
    var isAllOk = true
    val indices: Seq[IndexSetting] = Seq(
      Case, RestApi, Job, Project, Environment, Group, JobReport, Scenario
    )
    for (index <- indices if isAllOk) {
      logger.info(s"check es index ${index.Index}")
      isAllOk = IndexService.initCheck(index)
    }
    isAllOk
  }
}
