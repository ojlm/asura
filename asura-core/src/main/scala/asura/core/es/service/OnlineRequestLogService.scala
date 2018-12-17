package asura.core.es.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import asura.common.util.StringUtils
import asura.core.CoreConfig
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.cs.model.AggsItem
import asura.core.es.EsClient
import asura.core.es.model.{FieldKeys, FieldPattern, OnlineRequestLog, RestApiOnlineLog}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.Query

import scala.collection.mutable
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
      DomainOnlineConfigService.getConfig(domain).flatMap(config => {
        val inclusionPathItemMap = mutable.HashMap[String, FieldPattern]()
        var inMustQueries = ArrayBuffer[Query]()
        inMustQueries += termQuery(FieldKeys.FIELD_DOMAIN, domain)
        var notQueries: Seq[Query] = Nil
        var aggSize = apiCount
        if (null != config) {
          if (Option(config.maxApiCount).nonEmpty && config.maxApiCount > 0) {
            aggSize = config.maxApiCount
          }
          if (null != config.inclusions && config.inclusions.nonEmpty) {
            config.inclusions.foreach(item => {
              inMustQueries += fieldPatternToQuery(item)
              inclusionPathItemMap += (item.value -> item)
            })
          }
          if (null != config.exclusions && config.exclusions.nonEmpty) {
            notQueries = config.exclusions.map(fieldPatternToQuery(_))
          }
        }
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(CoreConfig.onlineLogDatePattern))
        val tuple = for {
          exclusion <- aggsItems(boolQuery().must(termQuery(FieldKeys.FIELD_DOMAIN, domain)).not(notQueries), yesterday, aggSize)
          inclusion <- aggsItems(boolQuery().must(inMustQueries), yesterday, aggSize)
        } yield (exclusion, inclusion)
        tuple.map(t => {
          val apiLogs = ArrayBuffer[RestApiOnlineLog]()
          t._1.foreach(item => {
            item.sub.foreach(subItem => {
              val percentage = Math.round(((subItem.count * 10000L).toDouble / domainTotal.toDouble)).toInt
              apiLogs += RestApiOnlineLog(domain, subItem.id, item.id, subItem.count, percentage)
            })
          })
          t._2.foreach(item => {
            item.sub.foreach(subItem => {
              val percentage = Math.round(((subItem.count * 10000L).toDouble / domainTotal.toDouble)).toInt
              if (inclusionPathItemMap.get(item.id).nonEmpty) {
                val pathAlias = inclusionPathItemMap.get(item.id).get.alias
                apiLogs += RestApiOnlineLog(domain, subItem.id, StringUtils.notEmptyElse(pathAlias, item.id), subItem.count, percentage)
              } else {
                apiLogs += RestApiOnlineLog(domain, subItem.id, item.id, subItem.count, percentage)
              }
            })
          })
          apiLogs
        })
      })
    } else {
      Future.successful(Nil)
    }
  }

  private def aggsItems(query: Query, date: String, aggSize: Int): Future[Seq[AggsItem]] = {
    EsClient.esOnlineLogClient.execute {
      search(s"${CoreConfig.onlineLogIndexPrefix}${date}")
        .query(query)
        .size(0)
        .aggregations(
          termsAgg(aggsTermName, OnlineRequestLog.KEY_URI).size(aggSize)
            .subAggregations(termsAgg(aggsTermName, OnlineRequestLog.KEY_METHOD))
        )
    }.map(toAggItems(_, null, OnlineRequestLog.KEY_METHOD))
  }

  private def fieldPatternToQuery(fieldPattern: FieldPattern): Query = {
    fieldPattern.`type` match {
      case FieldPattern.TYPE_TERM => termQuery(OnlineRequestLog.KEY_URI, fieldPattern.value)
      case FieldPattern.TYPE_WILDCARD => wildcardQuery(OnlineRequestLog.KEY_URI, fieldPattern.value)
      case FieldPattern.TYPE_REGEX => regexQuery(OnlineRequestLog.KEY_URI, fieldPattern.value)
      case _ => termQuery(OnlineRequestLog.KEY_URI, fieldPattern.value)
    }
  }
}
