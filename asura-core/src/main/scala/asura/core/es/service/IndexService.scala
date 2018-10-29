package asura.core.es.service

import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.EsClient
import asura.core.es.model.{IndexSetting, JobReportDataItem}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.Logger

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
    logger.info(s"check es template ${JobReportDataItem.Index}")
    val cli = EsClient.esClient
    val hasTpl = cli.execute {
      getIndexTemplate(JobReportDataItem.Index)
    }.map { res =>
      if (res.status != 404) true else false
    }.recover {
      case _ => false
    }.await
    if (!hasTpl) {
      val tplIndex = cli.execute {
        createIndexTemplate(JobReportDataItem.Index, s"${JobReportDataItem.Index}-*")
          .settings(Map(
            "number_of_replicas" -> JobReportDataItem.replicas,
            "number_of_shards" -> JobReportDataItem.shards
          ))
          .mappings(JobReportDataItem.mappings)
      }.await
      if (tplIndex.result.acknowledged) true else false
    } else {
      true
    }
  }

  def delIndex(indices: Seq[String]) = {
    EsClient.esClient.execute {
      deleteIndex(indices)
    }.map(toDeleteIndexResponse(_))
  }
}
