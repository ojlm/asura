package asura.app.api

import akka.actor.ActorSystem
import asura.common.util.StringUtils
import asura.core.cs.model.{AggsQuery, QueryDomain, QueryOnlineApi}
import asura.core.es.EsResponse
import asura.core.es.model.FieldKeys
import asura.core.es.service._
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OnlineLogApi @Inject()(implicit system: ActorSystem,
                             val exec: ExecutionContext,
                             val controllerComponents: SecurityComponents
                            ) extends BaseApi {

  def aggTerms() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    DomainOnlineLogService.aggTerms(aggs).toOkResult
  }

  def queryDomain() = Action(parse.byteString).async { implicit req =>
    val query = req.bodyAs(classOf[QueryDomain])
    val res = if (StringUtils.isEmpty(query.date)) {
      DomainOnlineLogService.aggTerms(AggsQuery(termsField = FieldKeys.FIELD_DATE, size = 30)).flatMap(dateAgg => {
        if (dateAgg.nonEmpty) {
          // order by date
          val ordered = dateAgg.sortWith((one, two) => one.id > two.id)
          query.date = ordered(0).id
          DomainOnlineLogService.queryDomain(query).map(esRes => {
            Map("dates" -> ordered, "domains" -> EsResponse.toApiData(esRes.result, false))
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
          val queryDomain = QueryDomain(names = Seq(query.domain), date = null)
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
}
