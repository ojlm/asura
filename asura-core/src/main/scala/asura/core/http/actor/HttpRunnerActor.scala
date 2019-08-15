package asura.core.http.actor

import akka.actor.Status.Success
import akka.actor.{ActorRef, Props}
import akka.pattern.pipe
import asura.common.actor.{BaseActor, ItemActorEvent, OverActorEvent, SenderMessage}
import asura.common.util.LogUtils
import asura.core.es.model.HttpCaseRequest
import asura.core.http.actor.HttpRunnerActor.{StepResult, TestCaseMessage}
import asura.core.http.{HttpResult, HttpRunner}
import asura.core.runtime.{ContextOptions, RuntimeContext}

import scala.concurrent.Future

class HttpRunnerActor extends BaseActor {

  implicit val ec = context.dispatcher
  var wsActor: ActorRef = null
  var docId: String = null
  var request: HttpCaseRequest = null
  var variables: Seq[java.util.Map[Any, Any]] = Nil

  override def receive: Receive = {
    case SenderMessage(wsSender) =>
      wsActor = wsSender
    case TestCaseMessage(docId, request, _) =>
      this.docId = docId
      this.request = request
      if (null != request.generator && null != request.generator.variables) {
        this.variables = request.generator.variables
      }
      if (this.variables.nonEmpty) {
        self ! 0
      } else {
        if (null != wsActor) wsActor ! OverActorEvent()
      }
    case idx: Int =>
      if (idx >= 0 && idx < this.variables.length) {
        execute(idx) pipeTo self
      } else {
        stopSelf()
      }
    case _ => stopSelf()
  }

  def execute(idx: Int): Future[Int] = {
    val varargs = this.variables(idx)
    val options = ContextOptions(initCtx = varargs)
    HttpRunner
      .test(this.docId, this.request, RuntimeContext(options = options))
      .map(result => {
        if (null != wsActor) wsActor ! ItemActorEvent(StepResult(idx, result))
        idx + 1
      })
      .recover {
        case t: Throwable =>
          val errMsg = LogUtils.stackTraceToString(t)
          if (null != wsActor) wsActor ! ItemActorEvent(StepResult(idx, null, errMsg))
          idx + 1
      }
  }

  def stopSelf(): Unit = {
    if (null != wsActor) wsActor ! Success
    context.stop(self)
  }
}

object HttpRunnerActor {

  def props() = Props(new HttpRunnerActor())

  case class TestCaseMessage(id: String, cs: HttpCaseRequest, options: ContextOptions)

  case class StepResult(idx: Int, result: HttpResult, errMsg: String = null)

}
