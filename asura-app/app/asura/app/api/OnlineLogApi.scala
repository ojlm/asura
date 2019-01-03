package asura.app.api

import akka.actor.ActorSystem
import asura.app.api.model.PreviewOnlineApi
import asura.common.util.StringUtils
import asura.core.ErrorMessages
import asura.core.cs.model.{AggsQuery, QueryDomain, QueryDomainWildcard, QueryOnlineApi}
import asura.core.es.model.{DomainOnlineConfig, FieldKeys}
import asura.core.es.service._
import asura.core.es.{EsClient, EsResponse}
import asura.core.job.impl.SyncOnlineDomainAndRestApiJob
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OnlineLogApi @Inject()(implicit system: ActorSystem,
                             val exec: ExecutionContext,
                             val controllerComponents: SecurityComponents
                            ) extends BaseApi {

  def domainAggTerms() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    DomainOnlineLogService.aggTerms(aggs).toOkResult
  }

  def domainWildcard() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryDomainWildcard])
    DomainOnlineLogService.queryDomainWildcard(query).toOkResultByEsList(false)
  }

  def queryDomain() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryDomain])
    val res = if (StringUtils.isEmpty(query.date)) {
      DomainOnlineLogService.aggTerms(AggsQuery(termsField = FieldKeys.FIELD_DATE, size = 30)).flatMap(dateAgg => {
        if (dateAgg.nonEmpty) {
          // order by date
          val ordered = dateAgg.sortWith((one, two) => one.id > two.id)
          query.date = ordered(0).id
          val tuple = for {
            countItems <- {
              query.sortField = FieldKeys.FIELD_COUNT
              DomainOnlineLogService.queryDomain(query)
            }
            coverageItems <- {
              query.sortField = FieldKeys.FIELD_COVERAGE
              DomainOnlineLogService.queryDomain(query)
            }
          } yield (countItems, coverageItems)
          tuple.map(counts => {
            Map(
              "dates" -> ordered,
              "count" -> EsResponse.toApiData(counts._1.result, false),
              "coverage" -> EsResponse.toApiData(counts._2.result, false),
            )
          })
        } else {
          Future.successful(Map.empty)
        }
      })
    } else {
      DomainOnlineLogService.queryDomain(query).map(esRes => {
        Map("domains" -> EsResponse.toApiData(esRes.result, false))
      })
    }
    res.toOkResult
  }

  def queryApi(hasDomain: Option[Boolean]) = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryOnlineApi])
    val res = if (hasDomain.nonEmpty && hasDomain.get) {
      val tupleRes = for {
        domain <- {
          val queryDomain = QueryDomain(names = Seq(query.domain), tag = query.tag, date = null)
          queryDomain.size = 30
          DomainOnlineLogService.queryDomain(queryDomain)
        }
        apis <- RestApiOnlineLogService.queryOnlineApiLog(query)
      } yield (domain, apis)
      tupleRes.map(tuple => {
        Map(
          "domain" -> EsResponse.toApiData(tuple._1.result, false),
          "apis" -> EsResponse.toApiData(tuple._2.result, false)
        )
      })
    } else {
      RestApiOnlineLogService.queryOnlineApiLog(query).map(esRes => {
        Map("apis" -> EsResponse.toApiData(esRes.result, false))
      })
    }
    res.toOkResult
  }

  def putDomainConfig() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[DomainOnlineConfig])
    doc.fillCommonFields(getProfileId())
    DomainOnlineConfigService.index(doc).toOkResult
  }

  def previewOnlineApiOfDomainConfig() = Action(parse.byteString).async { implicit req =>
    val preview = req.bodyAs(classOf[PreviewOnlineApi])
    val onlineEsLogConfigOpt = EsClient.esOnlineLogClient(preview.config.tag)
    if (onlineEsLogConfigOpt.nonEmpty) {
      OnlineRequestLogService.previewOnlineApi(preview.config, preview.domainTotal, SyncOnlineDomainAndRestApiJob.DEFAULT_API_COUNT, onlineEsLogConfigOpt.get).toOkResult
    } else {
      ErrorMessages.error_InvalidRequestParameters.toFutureFail
    }
  }

  def getDomainConfig(name: String) = Action.async { implicit req =>
    DomainOnlineConfigService.getConfig(name).toOkResult
  }

  def getApiMetrics() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryOnlineApi])
    RestApiOnlineLogService.getOnlineApiMetrics(query).toOkResult
  }
}
