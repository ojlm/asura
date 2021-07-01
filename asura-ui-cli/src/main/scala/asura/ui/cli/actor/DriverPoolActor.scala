package asura.ui.cli.actor

import java.io.File
import java.util
import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, Cancellable, Props, Terminated}
import akka.pattern.ask
import asura.common.actor.BaseActor
import asura.ui.UiConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.ui.cli.actor.DriverPoolActor.{GetDriverMessage, PoolOptions, ReleaseMessage, TaskOverMessage}
import asura.ui.cli.push.PushEventListener.{DriverCommandResultEvent, DriverPoolEvent}
import asura.ui.cli.push.PushOptions
import asura.ui.cli.server.ServerProxyConfig.ConcurrentHashMapPortSelector
import asura.ui.cli.task.TaskParams.{KarateParams, TaskType}
import asura.ui.cli.task._
import asura.ui.driver.DriverProvider
import com.intuit.karate.driver.{Driver, DriverOptions}
import com.intuit.karate.{FileUtils, Runner}

class DriverPoolActor(options: PoolOptions) extends BaseActor with DriverProvider {

  private var scheduler: Cancellable = null
  private val listener = if (options.push != null) options.push.buildClient() else null
  private val running = new ConcurrentHashMap[ActorRef, Driver]()
  private val idle = new ConcurrentHashMap[ActorRef, Driver]()
  private val runningTasks = new ConcurrentHashMap[String, TaskInfo]()

  override def receive: Receive = {
    case task: TaskInfo =>
      runTask(task)
    case TaskOverMessage(task) =>
      TaskInfo.remove()
      task.actors.foreach(actor => releaseChild(actor))
      if (task.meta != null && task.meta.reportId != null) {
        runningTasks.remove(task.meta.reportId, task)
      }
    case ReleaseMessage(actor) =>
      releaseChild(actor)
    case GetDriverMessage =>
      val drivers = idle.entrySet().iterator()
      if (drivers.hasNext) {
        val driver = drivers.next()
        sender() ! (driver.getKey, driver.getValue)
      } else {
        if (running.size() < options.coreCount) {
          startDriver(0, false, sender())
        } else {
          sender() ! null
        }
      }
    case Terminated(child) =>
      running.remove(child)
      idle.remove(child)
      log.info(s"driver(${child.path.address.toString}) is terminated.")
    case msg =>
      log.info(s"Unknown type: ${msg.getClass.getName}.")
  }

  override def preStart(): Unit = {
    DriverOptions.setDriverProvider(this)
    if (options.start) {
      if (options.maxCount == 1) {
        val port: Integer = if (options.ports != null && options.ports.size() == 1) options.ports.get(0) else 0
        startDriver(port, true, null)
      } else {
        0.until(options.initCount).foreach(_ => startDriver(0, true, null))
      }
    } else {
      if (options.ports != null) {
        options.ports.forEach(port => startDriver(port, true, null))
      }
    }
    startPushPoolStatus()
  }

  override def postStop(): Unit = {
    if (scheduler != null) scheduler.cancel()
    if (listener != null) listener.close()
  }

  private def releaseChild(actor: ActorRef): Unit = {
    var count = running.size() - options.coreCount
    if (count > 0) {
      context stop actor
      count = count - 1
    } else {
      actor ! DriverPoolItemActor.TaskOverMessage
      val driver = running.remove(actor)
      idle.put(actor, driver)
    }
  }

  private def runTask(task: TaskInfo): Unit = {
    if (task.params != null) {
      task.params.`type` match {
        case TaskType.KARATE => run(task.params.karate(), task)
        case TaskType.STOP => stop(task)
        case _ => throw new RuntimeException(s"Unknown task type: ${task.params.`type`}")
      }
    }
  }

  private def stop(task: TaskInfo): Unit = {
    if (task.meta != null && task.meta.reportId != null) {
      val runningTask = runningTasks.get(task.meta.reportId)
      if (runningTask != null && runningTask.hook != null) {
        runningTask.hook.stop()
      }
    }
  }

  private def run(params: KarateParams, task: TaskInfo): Unit = {
    val runnable = new Runnable {
      override def run(): Unit = {
        if (params.clean) {
          FileUtils.deleteDirectory(new File(params.output))
        }
        val builder = Runner.path(params.paths)
        val hook = new TaskRuntimeHook()
        task.hook = hook
        builder.hook(hook)
        builder.tags(params.tags)
        builder.scenarioName(params.name)
        builder.karateEnv(params.env)
        builder.workingDir(if (params.workingDir != null) new File(params.workingDir) else new File(""))
        builder.buildDir(params.output)
        builder.configDir(params.configDir)
        builder.outputHtmlReport(if (params.formats == null) true else !params.formats.contains("~html"))
        builder.outputCucumberJson(if (params.formats == null) false else params.formats.contains("cucumber:json"))
        builder.outputJunitXml(if (params.formats == null) false else params.formats.contains("junit:xml"))
        TaskInfo.set(task)
        if (task.meta != null && task.meta.reportId != null) {
          runningTasks.put(task.meta.reportId, task)
        }
        val results = builder.parallel(1)
        self ! TaskOverMessage(task)
        listener.driverCommandResultEvent(DriverCommandResultEvent(task.meta, true, results.toKarateJson))
      }
    }
    val thread = TaskThreadGroup.newKarate(runnable)
    thread.setUncaughtExceptionHandler((_: Thread, t: Throwable) => {
      self ! TaskOverMessage(task)
      listener.driverCommandResultEvent(DriverCommandResultEvent(task.meta, false, error = t.getMessage))
    })
    thread.start()
  }

  // called by the runner thread
  override def get(`type`: String, driverPptions: java.util.Map[String, AnyRef]): Driver = {
    import asura.common.util.FutureUtils.RichFuture
    val driver = (self ? GetDriverMessage).asInstanceOf[Future[(ActorRef, Driver)]].await(30 seconds)
    if (driver == null) {
      throw new RuntimeException("There is not enough resource")
    } else {
      val task = TaskInfo.get()
      if (task.drivers == null) task.drivers = TaskDrivers()
      if (task.actors == null) task.actors = mutable.Set[ActorRef]()
      if (task.driverActorMap == null) task.driverActorMap = mutable.Map[Driver, ActorRef]()
      if (options.push != null) {
        val push = options.push
        task.drivers.drivers += TaskDriver(push.pushIp, push.pushPort, driver._2.getOptions.port)
      }
      task.actors += driver._1
      task.driverActorMap += (driver._2 -> driver._1)
      driver._2
    }
  }

  override def release(driver: Driver): Unit = {
    val task = TaskInfo.get()
    if (task != null) { // called by the script runner thread
      val actor = task.driverActorMap(driver)
      task.actors -= actor
      task.driverActorMap -= driver
      self ! ReleaseMessage(actor)
    }
  }

  // only the 'port' is different, '0' for random.
  private def startDriver(port: Integer, isIdle: Boolean, sender: ActorRef): Future[(ActorRef, Driver)] = {
    val copied = new util.HashMap[String, Object](options.driver)
    copied.put("port", port)
    if (!copied.containsKey("userDataDir")) {
      copied.put("userDataDir", s"${options.userDataDirPrefix}/driver-${System.nanoTime()}")
    }
    val child = context.actorOf(DriverPoolItemActor.props(copied, options.selector, listener))
    child.ask(DriverPoolItemActor.GetDriverMessage)(timeout = 30 seconds, sender = Actor.noSender)
      .asInstanceOf[Future[Driver]]
      .map(driver => {
        context watch child
        if (isIdle) {
          idle.put(child, driver)
        } else {
          running.put(child, driver)
          sender ! (child, driver)
        }
        (child, driver)
      })(context.dispatcher)
  }

  private def startPushPoolStatus(): Unit = {
    if (listener != null && listener.options.pushStatus) {
      val pushOptions = listener.options
      scheduler = context.system.scheduler.scheduleWithFixedDelay(5 seconds, pushOptions.pushInterval seconds)(() => {
        import scala.jdk.CollectionConverters._
        val event = DriverPoolEvent(
          host = pushOptions.pushIp, port = pushOptions.pushPort,
          idle = idle.size, core = options.coreCount, running = running.size, max = options.maxCount,
          reports = runningTasks.keys().asScala.toSeq
        )
        listener.driverPoolEvent(event)
      })(context.dispatcher)
    }
  }

}

object DriverPoolActor {

  def props(options: PoolOptions) = Props(new DriverPoolActor(options))

  case class PoolOptions(
                          start: Boolean,
                          initCount: Int,
                          coreCount: Int,
                          maxCount: Int,
                          userDataDirPrefix: String,
                          removeUserDataDir: Boolean,
                          ports: java.util.List[Integer],
                          driver: java.util.HashMap[String, Object],
                          push: PushOptions,
                          selector: ConcurrentHashMapPortSelector
                        )

  case object GetDriverMessage

  case class TaskOverMessage(task: TaskInfo)

  case class ReleaseMessage(actor: ActorRef)

}
