package asura.ui.cli.push

import asura.common.util.HostUtils
import asura.ui.cli.push.PushEventListener._
import asura.ui.cli.task._
import asura.ui.karate.KarateResultModel
import asura.ui.model.{ChromeTargetPage, ChromeVersion}

trait PushEventListener {

  val options: PushOptions

  def driverPoolEvent(event: DriverPoolEvent): Unit

  def driverStatusEvent(event: DriverStatusEvent): Unit

  def driverTaskInfoEvent(event: DriverTaskInfoEvent): Unit

  def driverDevToolsEvent(event: DriverDevToolsEvent): Unit

  def driverCommandLogEvent(event: DriverCommandLogEvent): Unit

  def driverCommandResultEvent(event: DriverCommandResultEvent): Unit

  def close(): Unit = {}

}

object PushEventListener {

  val STATUS_IDLE = 0
  val STATUS_RUNNING = 1

  object MessageType {
    val DRIVER_COMMEND_EVENT = 0
    val DRIVER_POOL_EVENT = 1
    val DRIVER_STATUS_EVENT = 2
    val DRIVER_TASK_EVENT = 3
    val DRIVER_DEVTOOLS_EVENT = 4
    val DRIVER_COMMAND_LOG_EVENT = 5
    val DRIVER_COMMAND_RESULT_EVENT = 6
  }

  trait BasicEvent {
    val host: String
    val port: Integer
    var timestamp: Long = System.currentTimeMillis()
    var hostname: String = HostUtils.hostname
  }

  case class DriverPoolEvent(
                              host: String,
                              port: Integer,
                              idle: Integer,
                              core: Integer,
                              running: Integer,
                              max: Integer,
                              reports: Seq[String],
                              electron: Boolean,
                            ) extends BasicEvent

  case class DriverStatusEvent(
                                host: String,
                                port: Integer,
                                electron: Boolean,
                                var driverPort: Integer = 0,
                                var status: Int = STATUS_IDLE,
                                var task: TaskInfo = null,
                                var screen: String = null, // base64 image
                                var targets: Seq[ChromeTargetPage] = Nil,
                                var version: ChromeVersion = null,
                              ) extends BasicEvent

  case class TaskInfoEventItem(meta: TaskMeta, drivers: Seq[TaskDriver])

  case class DriverTaskInfoEvent(
                                  host: String,
                                  port: Integer,
                                  var tasks: Seq[TaskInfoEventItem] = null,
                                ) extends BasicEvent

  case class DriverDevToolsEvent(
                                  task: TaskMeta,
                                  params: TaskDevToolParams,
                                )

  case class DriverCommandLogEvent(
                                    task: TaskMeta,
                                    log: TaskLog,
                                  )

  case class DriverCommandResultEvent(
                                       task: TaskMeta,
                                       ok: Boolean,
                                       results: KarateResultModel = null,
                                       error: String = null,
                                     )

}
