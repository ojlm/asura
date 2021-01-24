package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig}
import asura.core.model.QueryFile
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object FileNodeService extends CommonService {

  def getById(id: String) = {
    EsClient.esClient.execute {
      search(FileNode.Index).query(idsQuery(id)).size(1)
    }
  }

  def getFileNodeById(id: String): Future[FileNode] = {
    getById(id).map(res => {
      if (res.isSuccess && res.result.nonEmpty) {
        JacksonSupport.parse(res.result.hits.hits(0).sourceAsString, classOf[FileNode])
      } else {
        throw ErrorMessages.error_IdNonExists.toException
      }
    })
  }

  def index(item: FileNode): Future[IndexDocResponse] = {
    val error = validate(item)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.esClient.execute {
        indexInto(FileNode.Index / EsConfig.DefaultType)
          .doc(item)
          .refresh(RefreshPolicy.WaitFor)
      }.map(toIndexDocResponse(_))
    }
  }

  def validate(item: FileNode): ErrorMessage = {
    if (null == item || StringUtils.hasEmpty(item.group, item.project, item.`type`, item.name)) {
      ErrorMessages.error_InvalidParams
    } else {
      null
    }
  }

  def fileExists(group: String, project: String, name: String, parentId: String) = {
    EsClient.esClient.execute {
      val esQueries = ArrayBuffer[Query]()
      esQueries += termQuery(FieldKeys.FIELD_GROUP, group)
      esQueries += termQuery(FieldKeys.FIELD_PROJECT, project)
      esQueries += termQuery(FieldKeys.FIELD_NAME, name)
      if (StringUtils.isNotEmpty(parentId)) {
        esQueries += termQuery(FieldKeys.FIELD_PARENT, parentId)
      } else {
        esQueries += not(existsQuery(FieldKeys.FIELD_PARENT))
      }
      count(FileNode.Index).filter {
        boolQuery().must(esQueries)
      }
    }
  }

  def queryDocs(query: QueryFile) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(query.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
    if (StringUtils.isNotEmpty(query.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, query.project)
    if (StringUtils.isNotEmpty(query.`type`)) esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    if (query.topOnly) {
      esQueries += must(not(existsQuery(FieldKeys.FIELD_PARENT)))
    } else {
      if (StringUtils.isNotEmpty(query.parent)) esQueries += termQuery(FieldKeys.FIELD_PARENT, query.parent)
    }
    if (StringUtils.isNotEmpty(query.name)) {
      esQueries += wildcardQuery(FieldKeys.FIELD_NAME, query.name)
    }
    if (StringUtils.isNotEmpty(query.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
    }
    EsClient.esClient.execute {
      search(FileNode.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortBy(FieldSort(FieldKeys.FIELD_TYPE).desc(), FieldSort(FieldKeys.FIELD_NAME).asc())
        .sourceExclude(Seq(FieldKeys.FIELD_DATA))
    }
  }

}
