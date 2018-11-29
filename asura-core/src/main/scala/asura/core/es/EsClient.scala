package asura.core.es

import asura.common.util.StringUtils
import asura.core.es.model._
import asura.core.es.service.IndexService
import com.sksamuel.elastic4s.embedded.LocalNode
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties}
import com.sksamuel.elastic4s.mappings.Analysis
import com.typesafe.scalalogging.Logger

object EsClient {

  val logger = Logger("EsClient")
  private var client: ElasticClient = _
  private var onlineLogClient: ElasticClient = _

  def esClient: ElasticClient = client

  def esOnlineLogClient = onlineLogClient

  /**
    * check if index exists, if not create
    */
  def init(useLocalNode: Boolean, url: String, dataDir: String): Boolean = {
    if (useLocalNode) {
      EsConfig.IK_ANALYZER = Analysis()
      if (StringUtils.isNotEmpty(url)) {
        client = ElasticClient(ElasticProperties(url))
      } else {
        val localNode = LocalNode("asura", dataDir)
        logger.info(s"start local es node: ${localNode.ipAndPort}")
        client = localNode.client(true)
      }
    } else {
      client = ElasticClient(ElasticProperties(url))
    }
    var isAllOk = true
    val indices: Seq[IndexSetting] = Seq(
      Case, RestApi, Job, Project, Environment,
      Group, JobReport, JobNotify, Scenario, UserProfile,
      Activity, DomainOnlineLog,
    )
    for (index <- indices if isAllOk) {
      logger.info(s"check es index ${index.Index}")
      isAllOk = IndexService.initCheck(index)
    }
    isAllOk = IndexService.checkTemplate()
    isAllOk
  }

  def initOnlineLogClient(url: String): Unit = {
    if (StringUtils.isNotEmpty(url)) {
      onlineLogClient = ElasticClient(ElasticProperties(url))
    }
  }
}
