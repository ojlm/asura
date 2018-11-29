package asura.core.es.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import asura.common.util.StringUtils
import asura.core.CoreConfig
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.AggsItem
import asura.core.es.EsClient
import asura.core.es.model.{OnlineRequestLog, RestApiOnlineLog}
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object OnlineRequestLogService extends CommonService with BaseAggregationService {

  def getOnlineDomain(domainCount: Int): Future[Seq[AggsItem]] = {
    if (null != EsClient.esOnlineLogClient && StringUtils.isNotEmpty(CoreConfig.onlineLogIndexPrefix)) {
      val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(CoreConfig.onlineLogDatePattern))
      EsClient.esClient.execute {
        search(s"${CoreConfig.onlineLogIndexPrefix}${yesterday}")
          .query(matchAllQuery())
          .size(0)
          .aggregations(termsAgg(aggsTermName, OnlineRequestLog.KEY_DOMAIN).size(domainCount))
      }.map(toAggItems(_, null))
    } else {
      Future.successful(Nil)
    }
  }

  def getOnlineApi(domain: String, apiCount: Int): Future[Seq[RestApiOnlineLog]] = {
    if (null != EsClient.esOnlineLogClient && StringUtils.isNotEmpty(CoreConfig.onlineLogIndexPrefix)) {
      val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(CoreConfig.onlineLogDatePattern))
      EsClient.esClient.execute {
        search(s"${CoreConfig.onlineLogIndexPrefix}${yesterday}")
          .query(matchAllQuery())
          .size(0)
          .aggregations(
            termsAgg(aggsTermName, OnlineRequestLog.KEY_DOMAIN).size(apiCount)
              .subAggregations(termsAgg(aggsTermName, OnlineRequestLog.KEY_METHOD))
          )
      }.map(toAggItems(_, OnlineRequestLog.KEY_METHOD))
        .map(items => {
          val apiLogs = ArrayBuffer[RestApiOnlineLog]()
          items.foreach(item => {
            item.sub.foreach(subItem => {
              apiLogs += RestApiOnlineLog(domain, subItem.`type`, subItem.id, subItem.count)
            })
          })
          apiLogs
        })
    } else {
      Future.successful(Nil)
    }
  }
}
