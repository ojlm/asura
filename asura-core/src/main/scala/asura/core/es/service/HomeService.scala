package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model._
import asura.core.model.QueryHome
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.queries.Query

import scala.collection.mutable.ArrayBuffer

object HomeService extends CommonService {

  val includeFields = Seq(
    FieldKeys.FIELD_GROUP,
    FieldKeys.FIELD_PROJECT,
    FieldKeys.FIELD_ID,
    FieldKeys.FIELD_AVATAR,
    FieldKeys.FIELD_SUMMARY,
    FieldKeys.FIELD_DESCRIPTION,
    FieldKeys.FIELD_OBJECT_REQUEST_URLPATH
  )

  def queryDoc(query: QueryHome) = {
    EsClient.esClient.execute {
      val esQueries = ArrayBuffer[Query]()
      if (StringUtils.isNotEmpty(query.text)) esQueries += matchQuery(FieldKeys.FIELD__TEXT, query.text)
      search(Group.Index, Project.Index, HttpCaseRequest.Index, DubboRequest.Index, SqlRequest.Index,
        Environment.Index, Scenario.Index, Job.Index)
        .query(boolQuery().must(esQueries))
        .sourceInclude(includeFields)
        .size(3)
    }
  }
}
