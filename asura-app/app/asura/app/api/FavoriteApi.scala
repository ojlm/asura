package asura.app.api

import akka.actor.ActorSystem
import asura.common.util.{DateUtils, StringUtils}
import asura.core.es.model.{Favorite, FieldKeys}
import asura.core.es.service._
import asura.core.model.{AggsQuery, QueryFavorite}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class FavoriteApi @Inject()(implicit system: ActorSystem,
                            val exec: ExecutionContext,
                            val controllerComponents: SecurityComponents
                           ) extends BaseApi {

  def exist() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[Favorite])
    doc.user = getProfileId()
    doc.timestamp = DateUtils.nowDateTime
    val docId = doc.generateDocId()
    FavoriteService.existDoc(docId).map(b => if (b) docId else StringUtils.EMPTY).toOkResult
  }

  def delete(id: String) = Action.async { implicit req =>
    FavoriteService.deleteDoc(id).toOkResult
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[Favorite])
    doc.user = getProfileId()
    doc.timestamp = DateUtils.nowDateTime
    FavoriteService.index(doc).toOkResult
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
}
