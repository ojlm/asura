package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{FieldKeys, IndexSetting, JobReportDataItem, RestApiOnlineLog}
import asura.core.es.{EsClient, EsConfig}
import com.sksamuel.elastic4s.IndexesAndTypes
import com.sksamuel.elastic4s.delete.DeleteByQueryRequest
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query
import com.typesafe.scalalogging.Logger

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object IndexService extends CommonService {

  val logger = Logger("IndexService")

  /** this will block current thread,it will only be used at startup */
  def initCheck(idx: IndexSetting): Boolean = {
    val cli = EsClient.esClient
    val res = cli.execute(indexExists(idx.Index)).await
    if (res.isSuccess) {
      if (res.result.exists) {
        true
      } else {
        val res2 = cli.execute {
          createIndex(idx.Index)
            .shards(idx.shards)
            .replicas(idx.replicas)
            .mappings(idx.mappings)
        }.await
        if (res2.isSuccess) {
          true
        } else {
          logger.error(res2.error.reason)
          false
        }
      }
    } else {
      logger.error(res.error.reason)
      false
    }
  }

  def checkTemplate(): Boolean = {
    checkIndexTemplate(JobReportDataItem).await && checkIndexTemplate(RestApiOnlineLog).await
  }

  def checkIndexTemplate(idxSetting: IndexSetting): Future[Boolean] = {
    logger.info(s"check es template ${idxSetting.Index}")
    val cli = EsClient.esClient
    cli.execute {
      getIndexTemplate(idxSetting.Index)
    }.map { res =>
      if (res.status != 404) true else false
    }.recover {
      case _ => false
    }.flatMap(hasTpl => {
      if (!hasTpl) {
        cli.execute {
          createIndexTemplate(idxSetting.Index, s"${idxSetting.Index}-*")
            .settings(Map(
              "number_of_replicas" -> idxSetting.replicas,
              "number_of_shards" -> idxSetting.shards
            ))
            .mappings(idxSetting.mappings)
        }.map(tplIndex => {
          if (tplIndex.result.acknowledged) true else false
        })
      } else {
        Future.successful(true)
      }
    })
  }

  def delIndex(indices: Seq[String]) = {
    EsClient.esClient.execute {
      deleteIndex(indices)
    }.map(toDeleteIndexResponse(_))
  }

  def deleteByGroupOrProject(indices: Seq[String], group: String, project: String) = {
    val esQueries = ArrayBuffer[Query]()
    if (StringUtils.isNotEmpty(group)) esQueries += termQuery(FieldKeys.FIELD_GROUP, group)
    if (StringUtils.isNotEmpty(project)) esQueries += termQuery(FieldKeys.FIELD_PROJECT, project)
    EsClient.esClient.execute {
      DeleteByQueryRequest(
        IndexesAndTypes(indices, Seq(EsConfig.DefaultType)),
        boolQuery().must(esQueries)
      ).refreshImmediately
    }.map(toDeleteByQueryResponse(_))
  }
}
