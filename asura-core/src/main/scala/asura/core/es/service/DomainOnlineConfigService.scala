package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model._
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.Future

object DomainOnlineConfigService extends CommonService {

  def index(item: DomainOnlineConfig): Future[IndexDocResponse] = {
    if (null == item && StringUtils.isEmpty(item.domain)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        indexInto(DomainOnlineConfig.Index).doc(item).id(item.domain)
      }.map(toIndexDocResponse(_))
    }
  }

  def getConfig(name: String): Future[DomainOnlineConfig] = {
    if (StringUtils.isEmpty(name)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        search(DomainOnlineConfig.Index).query(idsQuery(name)).size(1)
      }.map(res => {
        if (res.result.isEmpty) {
          null
        } else {
          JacksonSupport.parse(res.result.hits.hits(0).sourceAsString, classOf[DomainOnlineConfig])
        }
      })
    }
  }
}
