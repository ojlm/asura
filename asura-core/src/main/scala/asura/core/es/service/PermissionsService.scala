package asura.core.es.service

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.model.ApiMsg
import asura.common.util.{FutureUtils, JsonUtils, StringUtils}
import asura.core.ErrorMessages
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model._
import asura.core.model.QueryPermissions
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object PermissionsService extends CommonService with BaseAggregationService {

  def index(item: Permissions): Future[IndexDocResponse] = {
    val error = validate(item)
    if (null != error) {
      error.toFutureFail
    } else {
      EsClient.esClient.execute {
        indexInto(Permissions.Index)
          .doc(item)
          .refresh(RefreshPolicy.WAIT_FOR)
      }.map(toIndexDocResponse(_))
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.esClient.execute {
        delete(id).from(Permissions.Index).refresh(RefreshPolicy.WAIT_FOR)
      }.map(_ => toDeleteDocResponse(_))
    }
  }

  def updateDoc(id: String, doc: Permissions): Future[UpdateDocResponse] = {
    if (StringUtils.isEmpty(id) || null == doc) {
      ErrorMessages.error_EmptyId.toFutureFail
    } else {
      EsClient.esClient.execute {
        update(id).in(Permissions.Index).doc(JsonUtils.stringify(doc.toUpdateMap))
      }.map(toUpdateDocResponse(_))
    }
  }

  def validate(item: Permissions): ErrorMessage = {
    if (null == item ||
      StringUtils.hasEmpty(item.group, item.`type`, item.username, item.role) ||
      !item.isValidRole()
    ) {
      ErrorMessages.error_InvalidParams
    } else {
      null
    }
  }

  def queryDocs(query: QueryPermissions) = {
    val esQueries = ArrayBuffer[Query]()
    if (query.`type`.equals(Permissions.TYPE_GROUP)) {
      esQueries += termQuery(FieldKeys.FIELD_GROUP, query.group)
      esQueries += termQuery(FieldKeys.FIELD_TYPE, query.`type`)
    } else if (query.`type`.equals(Permissions.TYPE_PROJECT)) {
      esQueries += boolQuery().should(
        boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, query.group),
          termQuery(FieldKeys.FIELD_TYPE, Permissions.TYPE_GROUP)
        ),
        boolQuery().must(
          termQuery(FieldKeys.FIELD_GROUP, query.group),
          termQuery(FieldKeys.FIELD_PROJECT, query.project),
          termQuery(FieldKeys.FIELD_TYPE, Permissions.TYPE_PROJECT)
        )
      )
    }
    if (StringUtils.isNotEmpty(query.username)) esQueries += wildcardQuery(FieldKeys.FIELD_USERNAME, s"${query.username}*")
    EsClient.esClient.execute {
      search(Permissions.Index)
        .query(boolQuery().must(esQueries))
        .from(query.pageFrom)
        .size(query.pageSize)
        .sortByFieldDesc(FieldKeys.FIELD_UPDATED_AT)
    }
  }
}
