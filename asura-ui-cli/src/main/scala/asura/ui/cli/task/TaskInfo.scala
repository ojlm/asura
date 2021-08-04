package asura.ui.cli.task

import scala.collection.mutable

import akka.actor.ActorRef
import com.fasterxml.jackson.annotation.JsonIgnore
import com.intuit.karate.driver.Driver

case class TaskInfo(
                     meta: TaskMeta,
                     params: TaskParams,
                     var drivers: mutable.Set[TaskDriver] = mutable.Set(),
                     var startAt: Long = 0L,
                   ) {
  @JsonIgnore var actors = mutable.Set[ActorRef]()
  @JsonIgnore var driverActorMap = mutable.Map[Driver, ActorRef]()
  @JsonIgnore var hook: TaskRuntimeHook = null
  @JsonIgnore var thread: Thread = null
  @JsonIgnore var targets = mutable.Map[Driver, TaskDriver]()
}

object TaskInfo {

  private val threadLocal = new ThreadLocal[TaskInfo]()
  val EMPTY = TaskInfo(null, null)

  def get(): TaskInfo = threadLocal.get()

  def set(info: TaskInfo): Unit = threadLocal.set(info)

  def remove(): Unit = threadLocal.remove()

}
