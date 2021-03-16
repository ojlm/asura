package asura.core.es.actor

import java.util
import java.util.Collections

import akka.actor.{Props, Status}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{DateUtils, LogUtils}
import asura.core.actor.messages.Flush
import asura.core.es.actor.UiTaskListenerActor.WrappedLog
import asura.core.es.model.FormDataItem.BlobMetaData
import asura.core.es.model.{LogEntry, UiTaskReport}
import asura.core.es.service.{LogEntryService, UiTaskReportService}
import asura.core.store.BlobStoreEngine
import asura.ui.actor.ChromeDriverHolderActor._
import asura.ui.driver.{DevToolsProtocol, DriverCommandLog}
import asura.ui.model.BytesObject

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * @param bufferSize
 * @param flushInterval in ms
 */
class UiTaskListenerActor(imageStore: Option[BlobStoreEngine], bufferSize: Int, flushInterval: Int) extends BaseActor {

  implicit val ec = context.dispatcher
  val buffer = ArrayBuffer[WrappedLog]()

  override def receive: Receive = {

    case TaskListenerCreateMessage(command) => // create a report and return the reportId
      val now = DateUtils.nowDateTime
      val doc = UiTaskReport(
        summary = command.name,
        description = command.description,
        group = command.meta.group,
        project = command.meta.project,
        taskId = command.meta.taskId,
        `type` = command.`type`,
        params = command.params,
        servos = command.servos,
        startAt = command.meta.startAt,
        endAt = command.meta.startAt,
        creator = command.meta.creator,
        createdAt = now,
        updatedAt = now,
      )
      UiTaskReportService.index(doc).map(res => TaskListenerCreateMessageResponse(res.id, doc.day)) pipeTo sender()
    case TaskListenerEndMessage(meta, data) => // task finished
      UiTaskReportService.updateReport(meta, data)
    case TaskListenerDriverDevToolsMessage(data) => // dev tools logs
      self ! UiTaskListenerActor.devToolsMessageToLog(data)
    case TaskListenerDriverCommandLogMessage(data) => // command logs
      UiTaskListenerActor.driverCommandLogMessageToLog(data, imageStore) pipeTo self
    case log: WrappedLog => // eg: (2021.01.15, _)
      buffer += log
      if (buffer.length >= bufferSize) {
        self ! Flush
      }
    case Flush =>
      insert()
    case Status.Failure(t) =>
      log.warning(LogUtils.stackTraceToString(t))
    case msg =>
      log.warning(s"unknown msg type: ${msg.getClass.getName}")
  }

  override def preStart(): Unit = {
    context.system.scheduler.scheduleAtFixedRate(1 minute, flushInterval millis)(() => {
      self ! Flush
    })
  }

  override def postStop(): Unit = {
    insert()
    log.debug(s"${self.path} is stopped")
  }

  private def insert(): Unit = {
    if (buffer.length > 0) {
      log.debug(s"${buffer.length} log entry is saving...")
      LogEntryService.index(buffer.toSeq)
      buffer.clear()
    }
  }

}

object UiTaskListenerActor {

  case class WrappedLog(date: String, log: LogEntry)

  case class BytesObjectStore(locator: String, store: BlobMetaData, errMsg: String)

  def props(
             imageStore: Option[BlobStoreEngine],
             bufferSize: Int = 1000,
             flushInterval: Int = 5000,
           ) = {
    Props(new UiTaskListenerActor(imageStore, bufferSize, flushInterval))
  }

  def devToolsMessageToLog(msg: DriverDevToolsMessage): WrappedLog = {
    val meta = msg.meta
    val data = msg.params
    val log = LogEntry(
      group = meta.group, project = meta.project, taskId = meta.taskId, reportId = meta.reportId,
      `type` = LogEntry.TYPE_CONSOLE, hostname = meta.hostname, pid = meta.pid,
    )
    log.method = data.get(DevToolsProtocol.METHOD).asInstanceOf[String]
    val params = data.get("params")
    if (params != null) {
      val paramsMap = params.asInstanceOf[util.Map[Object, Object]]
      if (log.method.equals(DevToolsProtocol.METHOD_Log_entryAdded)) {
        val entry = paramsMap.get("entry")
        if (entry != null) {
          val entryMap = entry.asInstanceOf[util.Map[Object, Object]]
          val level = entryMap.get("level")
          if (level != null) {
            log.level = level.asInstanceOf[String]
            entryMap.remove("level")
          }
          val source = entryMap.get("source")
          if (source != null) {
            log.source = source.asInstanceOf[String]
            entryMap.remove("source")
          }
          val text = entryMap.get("text")
          if (null != text) {
            log.text = text.asInstanceOf[String]
            entryMap.remove("text")
          }
          val timestamp = entryMap.get("timestamp")
          if (timestamp != null) {
            log.timestamp = timestampToLong(timestamp)
            entryMap.remove("timestamp")
          }
          log.data = entryMap
        }
      } else {
        if (log.method.equals(DevToolsProtocol.METHOD_Runtime_consoleAPICalled)) {
          val typeObj = paramsMap.get("type")
          if (typeObj != null) {
            log.level = typeObj.asInstanceOf[String]
          }
          val argsObj = paramsMap.get("args")
          if (argsObj != null) {
            val sb = new StringBuilder()
            argsObj.asInstanceOf[java.util.List[java.util.Map[String, Object]]].forEach(arg => {
              val argType = arg.get("type")
              if ("string".equals(argType)) {
                sb.append(arg.get("value")).append(" ")
              } else if ("number".equals(argType)) {
                sb.append(arg.get("value").toString()).append(" ")
              }
            })
            if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1)
            log.text = sb.toString()
          }
        }
        val timestamp = paramsMap.get("timestamp")
        if (timestamp != null) {
          log.timestamp = timestampToLong(timestamp)
          paramsMap.remove("timestamp")
        }
        log.data = paramsMap
      }
    }
    if (log.timestamp <= 0) { // add a default timestamp
      log.timestamp = System.currentTimeMillis()
    }
    WrappedLog(meta.day, log)
  }

  def driverCommandLogMessageToLog(
                                    data: DriverCommandLog,
                                    imageStore: Option[BlobStoreEngine]
                                  )(implicit ec: ExecutionContext): Future[WrappedLog] = {
    val meta = data.meta
    val log = LogEntry(
      group = meta.group, project = meta.project, taskId = meta.taskId, reportId = meta.reportId,
      `type` = LogEntry.TYPE_MONKEY, hostname = meta.hostname, pid = meta.pid,
    )
    log.source = data.`type` // asura.ui.driver.DriverCommandLog
    log.timestamp = data.timestamp
    if (log.source.equals(DriverCommandLog.TYPE_SCREEN) && data.params.isInstanceOf[BytesObject]) {
      val params = data.params.asInstanceOf[BytesObject]
      if (imageStore.nonEmpty) {
        imageStore.get.uploadBytes(params.bytes).map(blobRes => {
          log.data = Collections.singletonMap("params", BytesObjectStore(params.locator, blobRes, null))
          WrappedLog(meta.day, log)
        }).recover({
          case t: Throwable =>
            log.data = Collections.singletonMap("params", BytesObjectStore(params.locator, null, t.getMessage))
            WrappedLog(meta.day, log)
        })
      } else {
        Future.successful(WrappedLog(meta.day, log))
      }
    } else {
      log.data = Collections.singletonMap("params", data.params)
      Future.successful(WrappedLog(meta.day, log))
    }
  }

  private def timestampToLong(timestamp: Object): Long = {
    if (timestamp.isInstanceOf[java.math.BigDecimal]) {
      timestamp.asInstanceOf[java.math.BigDecimal].longValue()
    } else if (timestamp.isInstanceOf[java.math.BigInteger]) {
      timestamp.asInstanceOf[java.math.BigInteger].longValue()
    } else if (timestamp.isInstanceOf[java.lang.Long]) {
      timestamp.asInstanceOf[java.lang.Long]
    } else if (timestamp.isInstanceOf[BigDecimal]) {
      timestamp.asInstanceOf[BigDecimal].toLong
    } else if (timestamp.isInstanceOf[Long]) {
      timestamp.asInstanceOf[Long]
    } else {
      0L
    }
  }

}
