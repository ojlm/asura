package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.FutureUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{BulkDocResponse, RestApiOnlineLog}
import asura.core.es.service.CommonService.CustomCatIndices
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.Future


object RestApiOnlineLogService extends CommonService {

  def index(items: Seq[RestApiOnlineLog], day: String): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(s"${RestApiOnlineLog.Index}-${day}" / EsConfig.DefaultType).doc(item))
        )
      }.map(toBulkDocResponse(_))
    }
  }

  def getIndices() = {
    EsClient.esClient.execute {
      CustomCatIndices(s"${RestApiOnlineLog.Index}-*")
    }
  }
}
