package asura.core.es

import asura.common.util.StringUtils
import asura.core.CoreConfig.EsOnlineLogConfig
import asura.core.es.model._
import asura.core.es.service.IndexService
import com.sksamuel.elastic4s.http.{ElasticClient, ElasticProperties, NoOpHttpClientConfigCallback}
import com.typesafe.scalalogging.Logger
import org.apache.http.client.config.RequestConfig
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback

import scala.collection.mutable

object EsClient {

  val logger = Logger("EsClient")
  private var client: ElasticClient = _
  private val onlineLogClient = mutable.Map[String, EsOnlineLogConfig]()

  def esClient: ElasticClient = client

  def esOnlineLogClient(tag: String) = onlineLogClient.get(tag)

  def esOnlineLogClients = onlineLogClient.values

  /**
   * check if index exists, if not create
   */
  def init(url: String): Boolean = {
    client = ElasticClient(ElasticProperties(url))
    var isAllOk = true
    val indices: Seq[IndexSetting] = Seq(
      HttpCaseRequest, Job, Project, Environment,
      Group, JobReport, JobNotify, Scenario, UserProfile,
      Activity, DomainOnlineLog, ProjectApiCoverage, DomainOnlineConfig,
      DubboRequest, SqlRequest, Favorite, CiTrigger, TriggerEventLog, Permissions
    )
    for (index <- indices if isAllOk) {
      logger.info(s"check es index ${index.Index}")
      isAllOk = IndexService.initCheck(index)
    }
    isAllOk = IndexService.checkTemplate()
    isAllOk
  }

  def initOnlineLogClient(configs: Seq[EsOnlineLogConfig]): Unit = {
    if (null != configs && configs.nonEmpty) {
      val clientCache = mutable.Map[String, ElasticClient]()
      configs.foreach(config => {
        if (StringUtils.isNotEmpty(config.url)) {
          config.onlineLogClient = clientCache.get(config.url).getOrElse({
            val client = ElasticClient(ElasticProperties(config.url), new CusRequestConfigCallback(), NoOpHttpClientConfigCallback)
            clientCache += (config.url -> client)
            client
          })
          onlineLogClient += (config.tag -> config)
        }
      })
    }
  }

  def closeClient(): Unit = {
    if (null != esClient) esClient.close()
    if (esOnlineLogClients.nonEmpty) esOnlineLogClients.foreach(config => {
      if (null != config.onlineLogClient) config.onlineLogClient.close()
    })
  }

  class CusRequestConfigCallback extends RequestConfigCallback {

    val connectionTimeout = 600000
    val socketTimeout = 600000

    override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
      // See https://github.com/elastic/elasticsearch/issues/24069
      // It's fixed in master now but still yet to release to 6.3.1
      requestConfigBuilder.setConnectionRequestTimeout(0)
        .setConnectTimeout(connectionTimeout)
        .setSocketTimeout(socketTimeout)
    }
  }

}
