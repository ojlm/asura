package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.FutureUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{BulkDocResponse, DomainOnlineLog}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.Future

object DomainOnlineLogService extends CommonService {

  def index(items: Seq[DomainOnlineLog]): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(DomainOnlineLog.Index / EsConfig.DefaultType).doc(item).id(item.generateDocId()))
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def syncOnlineDomain(domainCount: Int): Future[Seq[DomainOnlineLog]] = {
    var docs: Seq[DomainOnlineLog] = Nil
    OnlineRequestLogService.getOnlineDomain(domainCount).flatMap(items => {
      if (items.nonEmpty) {
        docs = items.map(item => DomainOnlineLog(item.id, item.count, item.`type`))
        index(docs).map(_ => docs)
      } else {
        Future.successful(Nil)
      }
    })
  }
}
