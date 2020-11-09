package asura.core.es.service

import asura.common.model.ApiMsg
import asura.common.util.FutureUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{BulkDocResponse, ProjectApiCoverage}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.Future

object ProjectApiCoverageService extends CommonService {

  def index(items: Seq[ProjectApiCoverage]): Future[BulkDocResponse] = {
    if (null == items && items.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(
          items.map(item => indexInto(ProjectApiCoverage.Index / EsConfig.DefaultType).doc(item).id(item.generateDocId()))
        )
      }.map(toBulkDocResponse(_))
    }
  }
}
