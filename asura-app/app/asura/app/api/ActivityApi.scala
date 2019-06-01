package asura.app.api

import akka.actor.ActorSystem
import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiRes
import asura.core.es.model.Activity
import asura.core.es.service._
import asura.core.model.AggsQuery
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ActivityApi @Inject()(implicit system: ActorSystem,
                            val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents
                           ) extends BaseApi {

  def trend(groups: Boolean = true) = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    aggs.types = Seq(Activity.TYPE_TEST_CASE)
    val res = for {
      trends <- ActivityService.trend(aggs)
    } yield trends
    res.map(trends => {
      OkApiRes(ApiRes(data = Map("trends" -> trends)))
    })
  }

  def aggTerms() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    ActivityService.aggTerms(aggs).toOkResult
  }

  def recent(wd: String = null, discover: Boolean = false) = Action(parse.byteString).async { implicit req =>
    RecommendService.getRecommendProjects(getProfileId(), wd, discover).toOkResult
  }
}
