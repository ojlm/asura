package asura.app.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import asura.app.api.model.TestDubbo
import asura.common.util.StringUtils
import asura.core.RunnerActors
import asura.core.cs.model.QueryDubboRequest
import asura.core.dubbo.DubboResult
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, DubboRequest}
import asura.core.es.service.DubboRequestService
import asura.dubbo.GenericRequest
import asura.dubbo.actor.GenericServiceInvokerActor.{GetInterfaceMethodParams, GetInterfacesMessage, GetProvidersMessage}
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class DubboApi @Inject()(
                          implicit val system: ActorSystem,
                          val exec: ExecutionContext,
                          val configuration: Configuration,
                          val controllerComponents: SecurityComponents,
                        ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())
  implicit val timeout: Timeout = 30.seconds
  val dubboInvoker = RunnerActors.dubboInvoker

  def getInterfaces() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[GetInterfacesMessage])
    (dubboInvoker ? msg).toOkResult
  }

  def getProviders() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[GetProvidersMessage])
    (dubboInvoker ? msg).toOkResult
  }

  def getParams() = Action(parse.byteString).async { implicit req =>
    val msg = req.bodyAs(classOf[GetInterfaceMethodParams])
    (dubboInvoker ? msg).toOkResult
  }

  def test() = Action(parse.byteString).async { implicit req =>
    val testMsg = req.bodyAs(classOf[TestDubbo])
    val dubboReq = testMsg.request
    val error = DubboRequestService.validate(dubboReq)
    if (null == error) {
      val user = getProfileId()
      activityActor ! Activity(dubboReq.group, dubboReq.project, user, Activity.TYPE_TEST_DUBBO, StringUtils.notEmptyElse(testMsg.id, StringUtils.EMPTY))
      (dubboInvoker ? toDubboGenericRequest(dubboReq)).flatMap(context => {
        DubboResult.evaluate(dubboReq, if (null == context) null else context.asInstanceOf[Object])
      }).toOkResult
    } else {
      error.toFutureFail
    }
  }

  def getById(id: String) = Action.async { implicit req =>
    DubboRequestService.getById(id).toOkResultByEsOneDoc(id)
  }

  def delete(id: String) = Action.async { implicit req =>
    DubboRequestService.deleteDoc(id).toOkResult
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[DubboRequest])
    val user = getProfileId()
    doc.fillCommonFields(user)
    DubboRequestService.index(doc).map(res => {
      activityActor ! Activity(doc.group, doc.project, user, Activity.TYPE_NEW_DUBBO, res.id)
      toActionResultFromAny(res)
    })
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val q = req.bodyAs(classOf[QueryDubboRequest])
    DubboRequestService.query(q).toOkResult
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[DubboRequest])
    DubboRequestService.updateDoc(id, doc).toOkResult
  }

  private def toDubboGenericRequest(req: DubboRequest): GenericRequest = {
    val parameterTypes = if (null != req.parameterTypes && req.parameterTypes.nonEmpty) {
      req.parameterTypes.map(_.`type`).toArray
    } else {
      null
    }
    val args = if (null != req.args && req.args.args.nonEmpty) {
      req.args.args.toArray
    } else {
      null
    }
    GenericRequest(
      dubboGroup = req.dubboGroup,
      interface = req.interface,
      method = req.method,
      parameterTypes = parameterTypes,
      args = args,
      address = req.address,
      port = req.port,
      version = req.version
    )
  }
}
