package asura.app.api

import akka.actor.ActorSystem
import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, Favorite, FieldKeys}
import asura.core.es.service._
import asura.core.model.{AggsQuery, QueryFavorite}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FavoriteApi @Inject()(implicit system: ActorSystem,
                            val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents
                           ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def exist() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[Favorite])
    doc.user = getProfileId()
    doc.timestamp = DateUtils.nowDateTime
    val logicId = doc.generateLogicId()
    FavoriteService.getByLogicId(logicId).map(response => {
      if (response.isSuccess) {
        EsResponse.toSingleApiData(response.result, true)
      } else {
        Map.empty
      }
    }).toOkResult
  }

  def uncheck(group: String, project: String, id: String) = Action.async { implicit req =>
    FavoriteService.check(id, false)
      .map(res => {
        activityActor ! Activity(group, project, getProfileId(), Activity.TYPE_TOP_TOP_UNCHECK, id)
        res
      })
      .toOkResult
  }

  def check() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[Favorite])
    val user = getProfileId()
    FavoriteService.getByLogicId(doc.generateLogicId())
      .flatMap(res => {
        if (res.result.hits.isEmpty) {
          doc.user = user
          doc.timestamp = DateUtils.nowDateTime
          FavoriteService.index(doc).map(_.id)
        } else {
          val hit = res.result.hits.hits(0)
          if (!hit.sourceAsMap.getOrElse(FieldKeys.FIELD_CHECKED, false).asInstanceOf[Boolean]) {
            FavoriteService.check(hit.id, true, doc.summary).map(_ => hit.id)
          } else {
            Future.successful(hit.id)
          }
        }
      })
      .map(docId => {
        activityActor ! Activity(doc.group, doc.project, user, Activity.TYPE_TOP_TOP_CHECK, docId)
        docId
      })
      .toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val q = req.bodyAs(classOf[QueryFavorite])
    FavoriteService.queryFavorite(q).toOkResultByEsList(true)
  }

  def groupAggs() = Action(parse.byteString).async { implicit req =>
    val q = AggsQuery(`type` = Favorite.TYPE_TOP_TOP, size = 100, termsField = FieldKeys.FIELD_GROUP)
    FavoriteService.termsAggs(q)
      .flatMap(aggsItems => {
        GroupService.getByIdsAsRawMap(aggsItems.map(_.id)).map(map => {
          val groupMap = map.asInstanceOf[Map[String, Map[String, Any]]]
          aggsItems.foreach(item => {
            if (groupMap.contains(item.id)) {
              item.summary = groupMap.get(item.id)
                .get.getOrElse(FieldKeys.FIELD_SUMMARY, StringUtils.EMPTY).asInstanceOf[String]
            }
          })
          aggsItems
        })
      }).toOkResult
  }

  def toptop(group: String, project: String, docId: String) = Action(parse.byteString).async { implicit req =>
    FavoriteService.getById(docId).flatMap(doc => {
      doc.targetType match {
        case Favorite.TARGET_TYPE_SCENARIO => ScenarioService.getRelativesById(doc.targetId)
        case Favorite.TARGET_TYPE_JOB => JobService.getRelativesById(doc.targetId)
        case _ => Future.successful(Map.empty)
      }
    }).toOkResult
  }
}
