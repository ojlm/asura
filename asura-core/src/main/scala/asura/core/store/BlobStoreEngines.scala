package asura.core.store

import scala.collection.mutable

object BlobStoreEngines {

  private val engines = mutable.HashMap[String, BlobStoreEngine]()

  /** this is not thread safe */
  def register(engine: BlobStoreEngine): Unit = {
    engines += (engine.name -> engine)
  }

  def get(name: String): Option[BlobStoreEngine] = engines.get(name)
}
