package asura.core.notify

import scala.collection.mutable.ArrayBuffer

object JobNotifyManager {

  private val manager = new java.util.concurrent.ConcurrentHashMap[String, JobNotifyFunction]()

  def register(func: JobNotifyFunction): Unit = {
    manager.put(func.`type`, func)
  }

  def get(`type`: String): Option[JobNotifyFunction] = {
    Option(manager.get(`type`))
  }

  def all(): Seq[JobNotifyItem] = {
    val notifiers = ArrayBuffer[JobNotifyItem]()
    manager.values().stream().forEach(func => notifiers += JobNotifyItem(func.`type`, func.description))
    notifiers.toSeq
  }

  case class JobNotifyItem(`type`: String, description: String)

}
