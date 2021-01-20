package asura.core.es.service

import asura.common.util.{DateUtils, JsonUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.job.JobExecDesc
import asura.core.model.QueryUiReport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import asura.ui.driver.{CommandMeta, DriverCommandEnd}
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object UiTaskReportService extends CommonService {

  def index(report: UiTaskReport): Future[IndexDocResponse] = {
    EsClient.esClient.execute {
      indexInto(UiTaskReport.Index / EsConfig.DefaultType).doc(report).refresh(RefreshPolicy.WaitFor)
    }.map(toIndexDocResponse(_))
  }

  def updateReport(meta: CommandMeta, data: DriverCommandEnd) = {
    val m = mutable.Map[String, Any]()
    m += (FieldKeys.FIELD_END_AT -> meta.endAt)
    m += (FieldKeys.FIELD_ELAPSE -> (meta.endAt - meta.startAt))
    if (!data.ok) {
      m += (FieldKeys.FIELD_RESULT -> JobExecDesc.STATUS_FAIL)
      m += (FieldKeys.FIELD_ERROR_MSG -> data.msg)
    }
    m += (FieldKeys.FIELD_DATA -> UiTaskReportData(data.result))
    m += (FieldKeys.FIELD_UPDATED_AT -> DateUtils.nowDateTime)
    EsClient.esClient.execute {
      update(meta.reportId).in(UiTaskReport.Index / EsConfig.DefaultType).doc(JsonUtils.stringify(m))
    }.map(toUpdateDocResponse(_))
  }

  def getById(id: String) = {
    EsClient.esClient.execute {
      search(UiTaskReport.Index).query(idsQuery(id)).size(1)
    }
  }


  def queryDocs(query: QueryUiReport) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    if (StringUtils.isNotEmpty(query.taskId)) {
      esQueries += wildcardQuery(FieldKeys.FIELD_TASK_ID, query.taskId)
    }
    EsClient.esClient.execute {
      search(UiTaskReport.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
        .sourceExclude(FieldKeys.FIELD_PARAMS, FieldKeys.FIELD_DATA)
    }
  }

}
