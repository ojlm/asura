package asura.core.es.service

import asura.core.es.EsClient
import asura.core.es.model.IndexSetting
import asura.core.es.service.GroupService.logger
import com.sksamuel.elastic4s.http.ElasticDsl._

object IndexService {

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

}
