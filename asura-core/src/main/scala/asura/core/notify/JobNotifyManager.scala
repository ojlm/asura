package asura.core.notify

import scala.collection.JavaConverters.collectionAsScalaIterable

object JobNotifyManager {

  private val manager = new java.util.concurrent.ConcurrentHashMap[String, JobNotifyFunction]()

  def register(func: JobNotifyFunction): Unit = {
    manager.put(func.`type`, func)
  }

  def get(`type`: String): Option[JobNotifyFunction] = {
    Option(manager.get(`type`))
  }

  def all(): Iterable[JobNotifyFunction] = collectionAsScalaIterable[JobNotifyFunction](manager.values())
}
