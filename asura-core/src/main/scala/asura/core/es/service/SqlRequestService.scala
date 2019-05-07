package asura.core.es.service

import java.nio.charset.StandardCharsets
import java.util.Base64

import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, RSAUtils, StringUtils}
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.model.QuerySqlRequest
import asura.core.es.model._
import asura.core.es.{EsClient, EsConfig, EsResponse}
import asura.core.sql.SqlParserUtils
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import asura.core.{CoreConfig, ErrorMessages}
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.sksamuel.elastic4s.searches.sort.FieldSort

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object SqlRequestService extends CommonService with BaseAggregationService {

  val queryFields = Seq(
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_CREATOR,
    FieldKeys.FIELD_CREATED_AT,
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_LABELS,
    FieldKeys.FIELD_HOST,
    FieldKeys.FIELD_PORT,
    FieldKeys.FIELD_DATABASE,
    FieldKeys.FIELD_TABLE
  )

  def index(doc: SqlRequest): Future[IndexDocResponse] = {
    val error = validate(doc)
    if (null == error) {
      EsClient.esClient.execute {
        indexInto(SqlRequest.Index / EsConfig.DefaultType).doc(doc).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toIndexDocResponse(_))
    } else {
      error.toFutureFail
    }
  }

  def deleteDoc(id: String): Future[DeleteDocResponse] = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(SqlRequest.Index).refresh(RefreshPolicy.WAIT_UNTIL)
      }.map(toDeleteDocResponse(_))
    }
  }

  def deleteDoc(ids: Seq[String]): Future[BulkDocResponse] = {
    if (null == ids || ids.isEmpty) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        bulk(ids.map(id => delete(id).from(SqlRequest.Index)))
      }.map(toBulkDocResponse(_))
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        search(SqlRequest.Index).query(idsQuery(id)).size(1)
      }
    }
  }

  private def getByIds(ids: Seq[String], filterFields: Boolean = false) = {
    if (null != ids) {
      EsClient.esClient.execute {
        search(SqlRequest.Index)
          .query(idsQuery(ids))
          .from(0)
          .size(ids.length)
          .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
          .sourceInclude(if (filterFields) queryFields else Nil)
      }
    } else {
      ErrorMessages.error_EmptyId.toFutureFail
    }
  }

  def getByIdsAsMap(ids: Seq[String], filterFields: Boolean = false): Future[Map[String, SqlRequest]] = {
    if (null != ids && ids.nonEmpty) {
      val map = mutable.HashMap[String, SqlRequest]()
      getByIds(ids, filterFields).map(res => {
        if (res.isSuccess) {
          if (res.result.isEmpty) {
            throw ErrorMessages.error_IdsNotFound(ids).toException
          } else {
            res.result.hits.hits.foreach(hit => map += (hit.id -> JacksonSupport.parse(hit.sourceAsString, classOf[SqlRequest])))
            map.toMap
          }
        } else {
          throw ErrorMessages.error_EsRequestFail(res).toException
        }
      })
    } else {
      Future.successful(Map.empty)
    }
  }

  def query(q: QuerySqlRequest): Future[Map[String, Any]] = {
    val esQueries = ArrayBuffer[Query]()
    var sortFields = Seq(FieldSort(FieldKeys.FIELD_CREATED_AT).desc())
    if (StringUtils.isNotEmpty(q.group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, q.group)
    if (StringUtils.isNotEmpty(q.project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, q.project)
    if (StringUtils.isNotEmpty(q.host)) esQueries += termQuery(FieldKeys.FIELD_HOST, q.host)
    if (StringUtils.isNotEmpty(q.database)) esQueries += termQuery(FieldKeys.FIELD_DATABASE, q.database)
    if (StringUtils.isNotEmpty(q.table)) esQueries += termQuery(FieldKeys.FIELD_TABLE, q.table)
    if (StringUtils.isNotEmpty(q.text)) {
      esQueries += matchQuery(FieldKeys.FIELD__TEXT, q.text)
      sortFields = Nil
    }
    if (StringUtils.isNotEmpty(q.sql)) esQueries += matchQuery(FieldKeys.FIELD_SQL, q.sql)
    EsClient.esClient.execute {
      search(SqlRequest.Index).query(boolQuery().must(esQueries))
        .from(q.pageFrom)
        .size(q.pageSize)
        .sortBy(sortFields)
        .sourceInclude(queryFields)
    }.flatMap(res => {
      fetchWithCreatorProfiles(res)
      if (res.isSuccess) {
        if (q.hasCreators) {
          fetchWithCreatorProfiles(res)
        } else {
          Future.successful(EsResponse.toApiData(res.result, true))
        }
      } else {
        ErrorMessages.error_EsRequestFail(res).toFutureFail
      }
    })
  }

  def updateDoc(id: String, doc: SqlRequest) = {
    if (StringUtils.isEmpty(id)) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      val error = validate(doc)
      if (null != error) {
        error.toFutureFail
      } else {
        EsClient.esClient.execute {
          val (src, params) = doc.toUpdateScriptParams
          update(id).in(SqlRequest.Index / EsConfig.DefaultType)
            .script {
              script(src).params(params)
            }
        }.map(toUpdateDocResponse(_))
      }
    }
  }

  def validate(doc: SqlRequest): ErrorMessages.ErrorMessage = {
    try {
      if (null == doc) {
        ErrorMessages.error_EmptyRequestBody
      } else {
        val table = SqlParserUtils.getStatementTable(doc.sql)
        doc.table = table
        val securityConfig = CoreConfig.securityConfig
        if (StringUtils.isNotEmpty(doc.password) && !doc.password.equals(securityConfig.maskText)) {
          // encrypt password
          val encryptedBytes = RSAUtils.encryptByPrivateKey(
            doc.password.getBytes(StandardCharsets.UTF_8), securityConfig.priKeyBytes)
          doc.encryptedPass = new String(Base64.getEncoder.encode(encryptedBytes))
          doc.password = securityConfig.maskText
        }
        if (StringUtils.isEmpty(doc.group)) {
          ErrorMessages.error_EmptyGroup
        } else if (StringUtils.isEmpty(doc.project)) {
          ErrorMessages.error_EmptyProject
        } else if (StringUtils.hasEmpty(doc.host, doc.username, doc.password, doc.encryptedPass, doc.database, doc.sql)) {
          ErrorMessages.error_InvalidRequestParameters
        } else {
          null
        }
      }
    } catch {
      case t: Throwable => ErrorMessages.error_Throwable(t)
    }
  }
}
