package asura.common.cache

import java.util

class LRUCache[K, V](size: Int, cb: (K, V) => Unit = null) extends util.LinkedHashMap[K, V](size + 1, 1.0f, true) {

  override def removeEldestEntry(eldest: util.Map.Entry[K, V]) = {
    val ret = size() > size
    if (ret && null != cb) {
      cb(eldest.getKey, eldest.getValue)
    }
    ret
  }
}

object LRUCache {

  def apply[K, V](size: Int, cb: (K, V) => Unit): LRUCache[K, V] = new LRUCache(size, cb)
}
