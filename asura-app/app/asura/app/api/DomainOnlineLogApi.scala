package asura.app.api

import akka.actor.ActorSystem
import asura.common.util.StringUtils
import asura.core.cs.model.{AggsQuery, QueryDomain}
import asura.core.es.EsResponse
import asura.core.es.model.FieldKeys
import asura.core.es.service._
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DomainOnlineLogApi @Inject()(implicit system: ActorSystem,
                                   val exec: ExecutionContext,
                                   val controllerComponents: SecurityComponents
                                  ) extends BaseApi {

  def aggTerms() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    DomainOnlineLogService.aggTerms(aggs).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
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
}
