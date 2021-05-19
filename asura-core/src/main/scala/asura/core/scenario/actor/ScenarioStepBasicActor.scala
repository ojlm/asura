package asura.core.scenario.actor

import java.util

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.util.Timeout
import asura.common.actor.{BaseActor, NotifyActorEvent}
import asura.common.util.{StringUtils, XtermUtils}
import asura.core.CoreConfig
import asura.core.assertion.engine.{AssertionContext, Statistic}
import asura.core.es.model.ScenarioStep
import asura.core.runtime.RuntimeContext
import asura.core.script.JsEngine

trait ScenarioStepBasicActor extends BaseActor {

  implicit val timeout: Timeout = CoreConfig.DEFAULT_ACTOR_ASK_TIMEOUT
  implicit val ec = context.dispatcher

  var runtimeContext: RuntimeContext
  var loopCount = 0

  // Actor which receive `asura.common.actor.ActorEvent` message.
  // Console log and step result will be sent to this actor which will influence the web ui
  // It should not be poisoned when running in a job
  var wsActor: ActorRef

  // jump to the target step when meet one of the conditions
  def handleJumpStep(step: ScenarioStep, idx: Int): Future[Int] = {
    var jumpTo = idx + 1
    if (null != step.data && null != step.data.jump) {
      val jump = step.data.jump
      if (jump.`type` == 0 && null != jump.conditions && jump.conditions.nonEmpty) {
        var meetCondition = false
        step.data.jump.conditions.foldLeft(Future.successful(meetCondition))((futureGoOn, condition) => {
          for {
            _ <- futureGoOn
            goOn <- {
              if (!meetCondition && null != condition && null != condition.assert && condition.assert.nonEmpty && condition.to > -1) {
                val statis = Statistic()
                AssertionContext.eval(condition.assert, runtimeContext.rawContext, statis)
                  .map(_ => if (statis.isSuccessful) {
                    jumpTo = sendJumpMsgAndGetJumpStep(condition.to, step, idx)
                    meetCondition = true
                    this.loopCount = this.loopCount + 1
                    meetCondition
                  } else {
                    meetCondition
                  })
              } else {
                Future.successful(false)
              }
            }
          } yield goOn
        }).map(_ => {
          if (!meetCondition && null != wsActor) {
            val jumpMsg = XtermUtils.blueWrap(s"No conditions meet, will continue ...")
            val msg = s"${consoleLogPrefix(step.`type`, idx)}${jumpMsg}"
            wsActor ! NotifyActorEvent(msg)
          }
          jumpTo
        })
      } else if (jump.`type` == 1 && StringUtils.isNotEmpty(jump.script)) {
        val script = jump.script
        Future.successful {
          val bindings = new util.HashMap[String, Any]()
          bindings.put(RuntimeContext.SELF_VARIABLE, runtimeContext.rawContext)
          if (null != wsActor) {
            /*JsEngine.localContext.get().getWriter.asInstanceOf[CustomWriter].log = (output) => {
              val msg = s"${consoleLogPrefix(step.`type`, idx)}${XtermUtils.blueWrap(output.toString)}"
              wsActor ! NotifyActorEvent(msg)
            }*/
          }
          val scriptResult = JsEngine.eval(script, bindings).asInstanceOf[Integer]
          if (null != scriptResult) {
            jumpTo = sendJumpMsgAndGetJumpStep(scriptResult, step, idx)
          } else {
            val jumpMsg = XtermUtils.blueWrap(s"Null script result, will continue ...")
            val msg = s"${consoleLogPrefix(step.`type`, idx)}${jumpMsg}"
            wsActor ! NotifyActorEvent(msg)
          }
          jumpTo
        }
      } else {
        Future.successful(jumpTo)
      }
    } else {
      Future.successful(jumpTo)
    }
  }

  // return -1 when need waiting
  def handleDelayStep(step: ScenarioStep, idx: Int): Future[Int] = {
    var next = idx + 1
    if (null != step.data && null != step.data.delay) {
      val condition = step.data.delay
      if (condition.value > 0) {
        val duration = condition.timeUnit match {
          case ScenarioStep.TIME_UNIT_MILLI => condition.value millis
          case ScenarioStep.TIME_UNIT_SECOND => condition.value seconds
          case ScenarioStep.TIME_UNIT_MINUTE => condition.value minutes
          case _ => null
        }
        if (null != duration) {
          next = -1
          if (null != wsActor) {
            val delayMsg = XtermUtils.blueWrap(s"Delay ${duration.toString} ...")
            val msg = s"${consoleLogPrefix(step.`type`, idx)}${delayMsg}"
            wsActor ! NotifyActorEvent(msg)
          }
          context.system.scheduler.scheduleOnce(duration, new Runnable {
            override def run(): Unit = self ! (idx + 1)
          })
        }
      }
    }
    Future.successful(next)
  }

  def consoleLogPrefix(stepType: String, idx: Int): String

  def sendJumpMsgAndGetJumpStep(expect: Int, step: ScenarioStep, idx: Int): Int
}
