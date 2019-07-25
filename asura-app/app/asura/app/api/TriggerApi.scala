package asura.app.api

import akka.actor.ActorSystem
import asura.common.util.StringUtils
import asura.core.es.EsResponse
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, CiTrigger, ScenarioStep}
import asura.core.es.service._
import asura.core.model.{QueryCiEvents, QueryTrigger}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggerApi @Inject()(
                            implicit val system: ActorSystem,
                            val exec: ExecutionContext,
                            val configuration: Configuration,
                            val controllerComponents: SecurityComponents,
                          ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())

  def getById(id: String) = Action.async { implicit req =>
    CiTriggerService.getTriggerById(id)
      .flatMap(trigger => {
        val tupleRes = for {
          readiness <- getReadiness(trigger)
          target <- getTarget(trigger)
        } yield (readiness, target)
        tupleRes.map(tuple => {
          Map("trigger" -> trigger, "readiness" -> tuple._1, "target" -> tuple._2)
        })
      })
      .toOkResult
  }

  def delete(id: String) = Action.async { implicit req =>
    CiTriggerService.deleteTrigger(id).toOkResult
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[CiTrigger])
    val user = getProfileId()
    doc.fillCommonFields(user)
    CiTriggerService.index(doc).map(res => {
      activityActor ! Activity(doc.group, doc.project, user, Activity.TYPE_NEW_TRIGGER, res.id)
      toActionResultFromAny(res)
    })
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryTrigger = req.bodyAs(classOf[QueryTrigger])
    CiTriggerService.queryTrigger(queryTrigger).toOkResultByEsList(true)
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[CiTrigger])
    CiTriggerService.updateDoc(id, doc).map(res => {
      activityActor ! Activity(doc.group, doc.project, getProfileId(), Activity.TYPE_UPDATE_TRIGGER, id)
      res
    }).toOkResult
  }

  def events() = Action(parse.byteString).async { implicit req =>
    val q = req.bodyAs(classOf[QueryCiEvents])
    TriggerEventLogService.queryEvents(q).toOkResultByEsList(true)
  }

  def getReadiness(trigger: CiTrigger): Future[Any] = {
    if (null != trigger.readiness && StringUtils.isNotEmpty(trigger.readiness.targetId)) {
      val docId = trigger.readiness.targetId
      trigger.readiness.targetType match {
        case ScenarioStep.TYPE_HTTP =>
          HttpCaseRequestService.getById(docId).map(res => {
            EsResponse.toSingleApiData(res.result, true)
          })
        case ScenarioStep.TYPE_DUBBO =>
          DubboRequestService.getById(docId).map(res => {
            EsResponse.toSingleApiData(res.result, true)
          })
        case ScenarioStep.TYPE_SQL =>
          SqlRequestService.getById(docId).map(res => {
            EsResponse.toSingleApiData(res.result, true)
          })
        case _ => Future.successful(Map.empty)
      }
    } else {
      Future.successful(Map.empty)
    }
  }

  def getTarget(trigger: CiTrigger): Future[Any] = {
    if (StringUtils.isNotEmpty(trigger.targetId)) {
      val docId = trigger.targetId
      trigger.targetType match {
        case ScenarioStep.TYPE_JOB =>
          JobService.getById(docId).map(res => {
            EsResponse.toSingleApiData(res.result, true)
          })
        case _ => Future.successful(Map.empty)
      }
    } else {
      Future.successful(Map.empty)
    }
  }
}
