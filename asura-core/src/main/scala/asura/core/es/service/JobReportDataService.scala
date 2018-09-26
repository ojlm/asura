package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.Future

object JobReportDataService extends CommonService {

  def index(items: Seq[(String, JobReportDataItem)], day: String): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(s"${JobReportDataItem.Index}-${day}" / EsConfig.DefaultType).doc(item._2).id(item._1))
        )
      }.map(toBulkDocResponse(_))
    }
  }


  def getById(id: String, day: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(s"${JobReportDataItem.Index}-${day}").query(idsQuery(id)).size(1)
      }
    }
  }

}
