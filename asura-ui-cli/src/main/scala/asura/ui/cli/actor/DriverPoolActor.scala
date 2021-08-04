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
import asura.ui.cli.actor.DriverPoolActor._
import asura.ui.cli.push.PushEventListener._
import asura.ui.cli.push.PushOptions
import asura.ui.cli.server.ServerProxyConfig.PortSelector
import asura.ui.cli.task.TaskParams.{KarateParams, TaskType}
import asura.ui.cli.task._
import asura.ui.driver.DriverProvider
import asura.ui.model.RemoteHost
import asura.ui.util.ChromeDevTools
import com.intuit.karate.core.ScenarioRuntime
import com.intuit.karate.driver.chrome.Chrome
import com.intuit.karate.driver.{Driver, DriverOptions}
import com.intuit.karate.{BuilderEx, FileUtils}

class DriverPoolActor(options: PoolOptions) extends BaseActor with DriverProvider {

  private var scheduler: Cancellable = null
  private val listener = if (options.push != null) options.push.buildClient() else null
  private val running = new ConcurrentHashMap[ActorRef, Driver]()
  private val idle = new ConcurrentHashMap[ActorRef, Driver]()
  private val runningTasks = new ConcurrentHashMap[String, TaskInfo]()

  override def receive: Receive = {
    case task: TaskInfo =>
      runTask(task)
    case GetTaskMessage(id) =>
      sender() ! runningTasks.getOrDefault(id, TaskInfo.EMPTY)
    case TaskOverMessage(task) =>
      TaskInfo.remove()
      task.actors.foreach(actor => releaseChild(actor, task.params.remote))
      if (task.meta != null && task.meta.reportId != null) {
        runningTasks.remove(task.meta.reportId, task)
      }
    case ReleaseMessage(actor, remote) =>
      releaseChild(actor, remote)
    case GetDriverMessage(driverOptions, sr, remote) =>
      if (remote == null) { // use local driver with resource limitation
        val drivers = idle.entrySet().iterator()
        if (drivers.hasNext) {
          val driver = drivers.next()
          idle.remove(driver.getKey)
          running.put(driver.getKey, driver.getValue)
          sender() ! ActorDriverTuple(driver.getKey, driver.getValue)
        } else {
          if (running.size() < options.maxCount) {
            startLocalDriver(0, false, sender())
          } else {
            sender() ! ActorDriverTuple(null, null)
          }
        }
      } else { // remote driver
        startRemoteDriver(driverOptions, sr, remote, sender())
      }
    case Terminated(child) =>
      running.remove(child)
      idle.remove(child)
      log.info(s"child driver(${child.path.toString}) is terminated.")
    case msg =>
      log.info(s"Unknown type: ${msg.getClass.getName}.")
  }

  override def preStart(): Unit = {
    DriverOptions.setDriverProvider(this)
    if (options.start) {
      if (options.maxCount == 1) {
        val port: Integer = if (options.ports != null && options.ports.size() == 1) options.ports.get(0) else 0
        startLocalDriver(port, true, null)
      } else {
        0.until(options.initCount).foreach(_ => startLocalDriver(0, true, null))
      }
    } else {
      if (options.ports != null) {
        options.ports.forEach(port => startLocalDriver(port, true, null))
      }
    }
    startPushPoolStatus()
  }

  override def postStop(): Unit = {
    if (scheduler != null) scheduler.cancel()
    if (listener != null) listener.close()
  }

  private def releaseChild(actor: ActorRef, remote: RemoteHost): Unit = {
    if (remote == null) { // local driver with resource limitation
      var count = running.size() - options.coreCount
      if (count > 0) {
        context stop actor
        count = count - 1
      } else {
        actor ! DriverPoolItemActor.TaskOverMessage
        val driver = running.remove(actor)
        if (driver != null) {
          idle.put(actor, driver)
        }
      }
    } else { // just stop
      context stop actor
    }
  }

  private def runTask(task: TaskInfo): Unit = {
    if (task.params != null) {
      task.params.`type` match {
        case TaskType.KARATE => run(task.params.karate(), task)
        case TaskType.STOP => stop(task)
        case _ => log.error(s"Unknown task type: ${task.params.`type`}")
      }
    }
  }

  private def stop(task: TaskInfo): Unit = {
    if (task.meta != null && task.meta.reportId != null) {
      val runningTask = runningTasks.get(task.meta.reportId)
      if (runningTask != null) {
        if (runningTask.hook != null) runningTask.hook.stop()
        if (runningTask.thread != null && runningTask.thread.isAlive) {
          runningTask.thread.interrupt()
        }
      }
    }
  }

  private def run(params: KarateParams, task: TaskInfo): Unit = {
    val runnable = new Runnable {
      override def run(): Unit = {
        if (params.clean) {
          val output = new File(params.output)
          if (output.exists() && output.isDirectory) {
            FileUtils.deleteDirectory(new File(params.output))
          }
        }
        val builder = BuilderEx.paths(params.paths)
        val hook = new TaskRuntimeHook(listener, task.meta)
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
        task.startAt = System.currentTimeMillis()
        TaskInfo.set(task)
        if (task.meta != null && task.meta.reportId != null) {
          runningTasks.put(task.meta.reportId, task)
        }
        val results = builder.parallel(1)
        self ! TaskOverMessage(task)
        if (listener != null) {
          listener.driverCommandResultEvent(DriverCommandResultEvent(task.meta, true, results.toKarateModel))
        }
      }
    }
    val thread = TaskThreadGroup.newKarate(runnable)
    task.thread = thread
    thread.setUncaughtExceptionHandler((thread: Thread, t: Throwable) => {
      log.error(t, s"thread error: ${thread.getName}")
      self ! TaskOverMessage(task)
      if (listener != null) {
        listener.driverCommandResultEvent(DriverCommandResultEvent(task.meta, false, error = t.getMessage))
      }
    })
    thread.start()
  }

  // called by the runner thread
  override def get(driverOptions: java.util.Map[String, AnyRef], sr: ScenarioRuntime): Driver = {
    import asura.common.util.FutureUtils.RichFuture
    import asura.ui.cli.CliSystem.ec
    val task = TaskInfo.get()
    val remote = task.params.remote
    val tuple = (self ? GetDriverMessage(driverOptions, sr, remote)).asInstanceOf[Future[ActorDriverTuple]].await(30 seconds)
    if (tuple.driver == null) {
      throw new RuntimeException("There is not enough resource")
    } else {
      if (task.meta != null && task.meta.reportId != null) {
        if (remote == null) {
          log.info(s"task(${task.meta.reportId}) get local driver, ${tuple.driver.getOptions.port}")
        } else {
          log.info(s"task(${task.meta.reportId}) get remote driver, ${remote.host}:${remote.port}")
        }
      }
      if (task.drivers == null) task.drivers = mutable.Set[TaskDriver]()
      if (task.actors == null) task.actors = mutable.Set[ActorRef]()
      if (task.driverActorMap == null) task.driverActorMap = mutable.Map[Driver, ActorRef]()
      if (task.targets == null) task.targets = mutable.Map[Driver, TaskDriver]()
      if (options.push != null) {
        val push = options.push
        val driverPort = tuple.driver.getOptions.port
        val targets = if (tuple.driver.isInstanceOf[Chrome]) {
          if (remote == null) {
            ChromeDevTools.getTargetPages("127.0.0.1", driverPort).await
          } else {
            ChromeDevTools.getTargetPages(remote.host, remote.port).await
          }
        } else {
          Nil
        }
        val taskDriver = TaskDriver(push.pushIp, push.pushPort, driverPort, driverOptions.getOrDefault("type", "chrome").toString, targets)
        task.drivers += taskDriver
        task.targets += (tuple.driver -> taskDriver)
      }
      tuple.actor ! DriverPoolItemActor.TaskInfoMessage(task)
      task.actors += tuple.actor
      task.driverActorMap += (tuple.driver -> tuple.actor)
      if (tuple.driver.isInstanceOf[Chrome] && remote == null) {
        // local chrome driver
        // fixme: reuse the `tuple.driver` is not working in linux, so recreate one
        // tuple.driver
        driverOptions.put("port", Int.box(tuple.driver.getOptions.port))
        driverOptions.put("start", Boolean.box(false))
        driverOptions.put("stop", Boolean.box(true))
        val child = Chrome.start(driverOptions, sr)
        child.parent = tuple.driver
        child
      } else {
        tuple.driver
      }
    }
  }

  // called by the runner thread, when driver.quit()
  override def release(driver: Driver): Unit = {
    val task = TaskInfo.get()
    if (task != null) {
      val managedDriver = if (driver.isInstanceOf[Chrome]) {
        val scriptDriver = driver.asInstanceOf[Chrome]
        if (scriptDriver.parent != null) { // local chrome only with ws client
          scriptDriver.closeClient()
          scriptDriver.parent
        } else {
          scriptDriver
        }
      } else {
        driver
      }
      if (task.driverActorMap.contains(managedDriver)) {
        // maybe release many times(e.g. ScenarioEngine.NewDriver.stopDriver which when is default)
        val actor = task.driverActorMap(managedDriver)
        task.actors -= actor
        task.driverActorMap -= managedDriver
        task.targets -= managedDriver
        self ! ReleaseMessage(actor, task.params.remote)
      }
    }
  }

  private def startRemoteDriver(
                                 options: java.util.Map[String, AnyRef],
                                 sr: ScenarioRuntime,
                                 remote: RemoteHost,
                                 sender: ActorRef,
                               ): Future[ActorDriverTuple] = {
    val child = context.actorOf(RemoteDriverActor.props(options, sr, remote, listener))
    child.ask(DriverPoolItemActor.GetDriverMessage)(timeout = 30 seconds, sender = Actor.noSender)
      .asInstanceOf[Future[Driver]]
      .map(driver => {
        context watch child
        val tuple = ActorDriverTuple(child, driver)
        if (sender != null) sender ! tuple
        tuple
      })(context.dispatcher)
  }

  // only the 'port' is different, '0' for random.
  private def startLocalDriver(port: Integer, isIdle: Boolean, sender: ActorRef): Future[ActorDriverTuple] = {
    val copied = new util.HashMap[String, Object](options.driver)
    copied.put("port", port)
    val electron = if (options.push != null) options.push.electron else false
    if (!copied.containsKey("userDataDir") && !electron) {
      copied.put("userDataDir", s"${options.userDataDirPrefix}/driver-${System.nanoTime()}")
    }
    val child = context.actorOf(DriverPoolItemActor.props(copied, options.selector, listener))
    child.ask(DriverPoolItemActor.GetDriverMessage)(timeout = 30 seconds, sender = Actor.noSender)
      .asInstanceOf[Future[Driver]]
      .map(driver => {
        context watch child
        val tuple = ActorDriverTuple(child, driver)
        if (isIdle) {
          idle.put(child, driver)
        } else {
          running.put(child, driver)
          sender ! tuple
        }
        tuple
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
          reports = runningTasks.keys().asScala.toSeq, electron = pushOptions.electron
        )
        listener.driverPoolEvent(event)
        val tasks = mutable.ArrayBuffer[TaskInfoEventItem]()
        runningTasks.forEach((_, task) => {
          if (task.targets != null && task.meta != null && task.meta.reportId != null) {
            tasks += TaskInfoEventItem(task.meta, task.targets.values.toSeq)
          }
        })
        if (tasks.nonEmpty) {
          val event = DriverTaskInfoEvent(pushOptions.pushIp, pushOptions.pushPort, tasks.toSeq)
          listener.driverTaskInfoEvent(event)
        }
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
                          selector: PortSelector
                        )

  case class ActorDriverTuple(actor: ActorRef, driver: Driver)

  case class GetDriverMessage(options: java.util.Map[String, AnyRef], sr: ScenarioRuntime, remote: RemoteHost)

  case class GetTaskMessage(id: String)

  case class TaskOverMessage(task: TaskInfo)

  case class ReleaseMessage(actor: ActorRef, remote: RemoteHost)

}
