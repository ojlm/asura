package asura.core.es.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import asura.common.util.StringUtils
import asura.core.CoreConfig
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.AggsItem
import asura.core.es.EsClient
import asura.core.es.model.{FieldKeys, OnlineRequestLog, RestApiOnlineLog}
import com.sksamuel.elastic4s.http.ElasticDsl._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object OnlineRequestLogService extends CommonService with BaseAggregationService {

  def getOnlineDomain(domainCount: Int, date: String): Future[Seq[AggsItem]] = {
    if (null != EsClient.esOnlineLogClient && StringUtils.isNotEmpty(CoreConfig.onlineLogIndexPrefix)) {
      EsClient.esOnlineLogClient.execute {
        search(s"${CoreConfig.onlineLogIndexPrefix}${date}")
          .query(matchAllQuery())
          .size(0)
          .aggregations(termsAgg(aggsTermName, OnlineRequestLog.KEY_DOMAIN).size(domainCount))
      }.map(toAggItems(_, date, null))
    } else {
      Future.successful(Nil)
    }
  }

  def getOnlineApi(domain: String, domainTotal: Long, apiCount: Int): Future[Seq[RestApiOnlineLog]] = {
    if (null != EsClient.esOnlineLogClient && StringUtils.isNotEmpty(CoreConfig.onlineLogIndexPrefix)) {
      val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(CoreConfig.onlineLogDatePattern))
      EsClient.esOnlineLogClient.execute {
        search(s"${CoreConfig.onlineLogIndexPrefix}${yesterday}")
          .query(termQuery(FieldKeys.FIELD_DOMAIN, domain))
          .size(0)
          .aggregations(
            termsAgg(aggsTermName, OnlineRequestLog.KEY_URI).size(apiCount)
              .subAggregations(termsAgg(aggsTermName, OnlineRequestLog.KEY_METHOD))
          )
      }.map(toAggItems(_, null, OnlineRequestLog.KEY_METHOD))
        .map(items => {
          val apiLogs = ArrayBuffer[RestApiOnlineLog]()
          items.foreach(item => {
            item.sub.foreach(subItem => {
              val percentage = Math.round(((subItem.count * 10000L).toDouble / domainTotal.toDouble)).toInt
              apiLogs += RestApiOnlineLog(domain, subItem.id, item.id, subItem.count, percentage)
            })
          })
          apiLogs
        })
    } else {
      Future.successful(Nil)
    }
  }
}
