package asura.core.es.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import asura.common.util.StringUtils
import asura.core.CoreConfig.EsOnlineLogConfig
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model._
import asura.core.es.service.BaseAggregationService._
import asura.core.model.AggsItem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.searches.aggs.AbstractAggregation
import com.sksamuel.elastic4s.requests.searches.queries.Query

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object OnlineRequestLogService extends CommonService with BaseAggregationService {

  def getOnlineDomain(domainCount: Int, date: String, esConfig: EsOnlineLogConfig): Future[Seq[DomainOnlineLog]] = {
    if (null != esConfig.onlineLogClient && StringUtils.isNotEmpty(esConfig.prefix)) {
      esConfig.onlineLogClient.execute {
        search(s"${esConfig.prefix}${date}")
          .query(matchAllQuery())
          .size(0)
          .aggregations(termsAgg(aggsTermsName, esConfig.fieldDomain).size(domainCount))
      }.map(toAggItems(_, date, null))
        .map(aggsItems => {
          if (aggsItems.nonEmpty) {
            aggsItems.map(item => DomainOnlineLog(item.id, esConfig.tag, item.count, 0, item.`type`))
          } else {
            Nil
          }
        })
    } else {
      Future.successful(Nil)
    }
  }

  def getOnlineApi(domain: String, domainTotal: Long, apiCount: Int, esConfig: EsOnlineLogConfig): Future[Seq[RestApiOnlineLog]] = {
    if (null != esConfig.onlineLogClient && StringUtils.isNotEmpty(esConfig.prefix)) {
      DomainOnlineConfigService.getConfig(domain).flatMap(config => {
        if (null == config) {
          previewOnlineApi(DomainOnlineConfig(null, null, domain, esConfig.tag, 0), domainTotal, apiCount, esConfig)
        } else {
          previewOnlineApi(config, domainTotal, apiCount, esConfig)
        }
      })
    } else {
      Future.successful(Nil)
    }
  }

  def previewOnlineApi(
                        config: DomainOnlineConfig,
                        domainTotal: Long,
                        apiCount: Int,
                        esConfig: EsOnlineLogConfig
                      ): Future[Seq[RestApiOnlineLog]] = {
    if (null != esConfig.onlineLogClient && StringUtils.isNotEmpty(esConfig.prefix)) {
      var notQueries = ArrayBuffer[Query]()
      val domain = config.domain
      var aggSize = apiCount
      if (null != config) {
        if (Option(config.maxApiCount).nonEmpty && config.maxApiCount > 0) {
          aggSize = config.maxApiCount
        }
        if (null != config.exclusions && config.exclusions.nonEmpty) {
          config.exclusions.foreach(item => {
            notQueries += fieldPatternToQuery(item, esConfig)
          })
        }
        if (null != config.exMethods && config.exMethods.nonEmpty) {
          config.exMethods.filter(method => StringUtils.isNotEmpty(method.name)).foreach(method => {
            notQueries += termQuery(esConfig.fieldMethod, method.name)
          })
        }
        if (StringUtils.isNotEmpty(esConfig.fieldRemoteAddr) && null != esConfig.excludeRemoteAddrs && esConfig.excludeRemoteAddrs.nonEmpty) {
          notQueries += termsQuery(esConfig.fieldRemoteAddr, esConfig.excludeRemoteAddrs)
        }
      }
      val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern(esConfig.datePattern))
      val tuple = for {
        exclusion <- aggsItems(
          boolQuery().must(termQuery(esConfig.fieldDomain, domain)).not(notQueries),
          yesterday,
          aggSize,
          toMetricsAggregation(esConfig.fieldRequestTime),
          esConfig
        )
        inclusion <- getInclusions(domain, yesterday, config, toMetricsAggregation(esConfig.fieldRequestTime), esConfig)
      } yield (exclusion, inclusion)
      tuple.map(t => {
        val minReqCount = if (Option(config.minReqCount).nonEmpty && config.minReqCount > 0) {
          config.minReqCount
        } else {
          -1
        }
        val exFileSuffixes: Set[String] = if (StringUtils.isNotEmpty(config.exSuffixes)) {
          config.exSuffixes.split(",").toSet
        } else {
          null
        }
        val apiLogs = ArrayBuffer[RestApiOnlineLog]()
        val itemFunc = (aggsItem: AggsItem) => {
          aggsItem.sub
            .filter(subItem => subItem.count >= minReqCount && !isOneOfSuffix(aggsItem.id, exFileSuffixes))
            .foreach(subItem => {
              val percentage = if (domainTotal > 0) Math.round(((subItem.count * 10000L).toDouble / domainTotal.toDouble)).toInt else 0
              apiLogs += RestApiOnlineLog(domain, esConfig.tag, subItem.id, aggsItem.id, subItem.count, percentage, metrics = subItem.metrics)
            })
        }
        t._1.foreach(itemFunc)
        t._2.foreach(itemFunc)
        apiLogs.toSeq
      })
    } else {
      Future.successful(Nil)
    }
  }

  private def isOneOfSuffix(path: String, suffixes: Set[String]): Boolean = {
    if (null != suffixes && path != null) {
      suffixes.exists(path.endsWith(_))
    } else {
      false
    }
  }

  private def getInclusions(
                             domain: String,
                             date: String,
                             config: DomainOnlineConfig,
                             metricsAggregations: Seq[AbstractAggregation] = Nil,
                             esConfig: EsOnlineLogConfig,
                           ): Future[collection.Seq[AggsItem]] = {
    if (null != config && null != config.inclusions && config.inclusions.nonEmpty) {
      val domainTerm = termQuery(esConfig.fieldDomain, domain)
      val notQueries = ArrayBuffer[Query]()
      if (null != config.exMethods) {
        notQueries += termsQuery(esConfig.fieldMethod, config.exMethods.filter(method => StringUtils.isNotEmpty(method.name)))
      }
      if (StringUtils.isNotEmpty(esConfig.fieldRemoteAddr) && null != esConfig.excludeRemoteAddrs && esConfig.excludeRemoteAddrs.nonEmpty) {
        notQueries += termsQuery(esConfig.fieldRemoteAddr, esConfig.excludeRemoteAddrs)
      }
      val items = ArrayBuffer[AggsItem]()
      config.inclusions.foldLeft(Future.successful(items))((itemsFuture, inclusion) => {
        for {
          items <- itemsFuture
          item <- {
            esConfig.onlineLogClient.execute {
              search(s"${esConfig.prefix}${date}")
                .query(boolQuery()
                  .must(domainTerm, fieldPatternToQuery(inclusion, esConfig))
                  .not(notQueries)
                )
                .size(0)
                .aggregations(
                  termsAgg(aggsTermsName, esConfig.fieldMethod)
                    .subAggregations(metricsAggregations)
                )
            }.map(toAggItems(_, null, null, esConfig.fieldRequestTimeResolution))
              .map(AggsItem(null, StringUtils.notEmptyElse(inclusion.alias, inclusion.value), 0, _))
          }} yield items += item
      })
    } else {
      Future.successful(Nil)
    }
  }

  private def aggsItems(
                         query: Query,
                         date: String,
                         aggSize: Int,
                         metricsAggregations: Seq[AbstractAggregation] = Nil,
                         esConfig: EsOnlineLogConfig,
                       ): Future[Seq[AggsItem]] = {
    esConfig.onlineLogClient.execute {
      search(s"${esConfig.prefix}${date}")
        .query(query)
        .size(0)
        .aggregations(
          termsAgg(aggsTermsName, esConfig.fieldUri).size(aggSize)
            .subAggregations(
              termsAgg(aggsTermsName, esConfig.fieldMethod)
                .subAggregations(metricsAggregations)
            )
        )
    }.map(toAggItems(_, null, esConfig.fieldMethod, esConfig.fieldRequestTimeResolution))
  }

  private def fieldPatternToQuery(fieldPattern: FieldPattern, esConfig: EsOnlineLogConfig): Query = {
    fieldPattern.`type` match {
      case FieldPattern.TYPE_TERM => termQuery(esConfig.fieldUri, fieldPattern.value)
      case FieldPattern.TYPE_WILDCARD => wildcardQuery(esConfig.fieldUri, fieldPattern.value)
      case FieldPattern.TYPE_REGEX => regexQuery(esConfig.fieldUri, fieldPattern.value)
      case _ => termQuery(esConfig.fieldUri, fieldPattern.value)
    }
  }
}
