package asura.core.es.service

import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.actor.UiTaskListenerActor.WrappedLog
import asura.core.es.model.{BulkDocResponse, LogEntry}
import asura.core.es.service.CommonService.CustomCatIndices
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.concurrent.Future

object LogEntryService extends CommonService {

  def index(items: Seq[WrappedLog]): Future[BulkDocResponse] = {
    EsClient.esClient.execute {
      bulk(
        items.map(item => indexInto(s"${LogEntry.Index}-${item.date}" / EsConfig.DefaultType).doc(item.log))
      )
    }.map(toBulkDocResponse(_))
  }

  def getIndices() = {
    EsClient.esClient.execute {
      CustomCatIndices(s"${LogEntry.Index}-*")
    }
  }

}
