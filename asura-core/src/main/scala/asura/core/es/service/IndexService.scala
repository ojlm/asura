package asura.core.es.service

import asura.core.es.EsClient
import asura.core.es.model.IndexSetting
import asura.core.es.service.GroupService.logger
import com.sksamuel.elastic4s.http.ElasticDsl._

object IndexService {

  /** this will block current thread,it will only be used at startup */
  def initCheck(idx: IndexSetting): Boolean = {
    val cli = EsClient.httpClient
    cli.execute {
      indexExists(idx.Index)
    }.await match {
      case Right(res) =>
        if (res.result.exists) {
          true
        } else {
          cli.execute {
            createIndex(idx.Index)
              .shards(idx.shards)
              .replicas(idx.replicas)
              .mappings(idx.mappings)
          }.await match {
            case Right(_) =>
              true
            case Left(res) =>
              logger.error(res.error.reason)
              false
          }
        }
      case Left(res) =>
        logger.error(res.error.reason)
        false
    }
  }

}
