package asura.core.es.service

import asura.common.util.StringUtils
import asura.core.concurrent.ExecutionContextManager.sysGlobal
import asura.core.es.model.{FieldKeys, Project}
import asura.core.model.RecommendProject

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object RecommendService {

  def getRecommendProjects(user: String, wd: String, discover: Boolean): Future[RecommendProjects] = {
    val futureTuple = for {
      my <- getRecommendProject(user, true, wd, 20, Nil)
      other <- if (discover) {
        getRecommendProject(user, false, null, 5, my.map(p => ((p.group, p.project))))
      } else {
        Future.successful(Nil)
      }
    } yield (my, other)
    futureTuple.flatMap(tuple => {
      val groupIds = ArrayBuffer[String]()
      tuple._1.foreach(item => groupIds += item.group)
      tuple._2.foreach(item => groupIds += item.group)
      GroupService.getByIdsAsRawMap(groupIds).map(groups => RecommendProjects(tuple._1, tuple._2, groups))
    })
  }

  def getRecommendProject(user: String, me: Boolean, wd: String, size: Int, excludeGPs: Seq[(String, String)]): Future[Seq[RecommendProject]] = {
    val items = ArrayBuffer[RecommendProject]()
    ActivityService.recentProjects(user, me, wd, size).flatMap(aggItems => {
      if (aggItems.nonEmpty) {
        val map = mutable.Map[String, RecommendProject]()
        aggItems.foreach(item => {
          if (StringUtils.isNotEmpty(item.id)) {
            val gp = item.id.split("/")
            if (gp.length == 2) {
              val project = RecommendProject(gp(0), gp(1), item.count)
              items += project
              map += (Project.generateDocId(gp(0), gp(1)) -> project)
            }
          }
        })
        ProjectService.getByIds(map.keys.toSeq, Seq(FieldKeys.FIELD_SUMMARY, FieldKeys.FIELD_DESCRIPTION)).map(idMap => {
          idMap.foreach(tuple => {
            val id = tuple._1
            val project = tuple._2
            map(id).summary = project.summary
            map(id).description = project.description
          })
          items
        })
      } else {
        Future.successful(items)
      }
    })
  }

  case class RecommendProjects(
                                my: Seq[RecommendProject],
                                others: Seq[RecommendProject],
                                groups: Map[_ <: String, Map[String, Any]],
                              )

}
