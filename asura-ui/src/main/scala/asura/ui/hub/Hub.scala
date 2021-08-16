package asura.ui.hub

import java.util.concurrent.ConcurrentHashMap

class Hub[T] {

  private val sinks = new ConcurrentHashMap[String, ConcurrentHashMap[Sink[T], Sink[T]]]()

  def getSinks(name: String): ConcurrentHashMap[Sink[T], Sink[T]] = {
    if (sinks.containsKey(name)) {
      sinks.get(name)
    } else {
      val map = new ConcurrentHashMap[Sink[T], Sink[T]]()
      val sink = sinks.putIfAbsent(name, map)
      if (sink == null) map else sink
    }
  }

  def closeAndRemoveSinks(name: String): Unit = {
    getSinks(name).keySet().forEach(sink => sink.close())
    sinks.remove(name)
  }

  def enter(name: String, sink: Sink[T]): Unit = {
    getSinks(name).put(sink, sink)
  }

  def leave(name: String, sink: Sink[T]): Unit = {
    getSinks(name).remove(sink)
  }

  def active(name: String, params: Object): Unit = {
    getSinks(name).keySet().forEach(sink => sink.active(params))
  }

  def write(sinks: ConcurrentHashMap[Sink[T], Sink[T]], frame: T): Unit = {
    sinks.keySet().forEach(sink => sink.write(frame))
  }

  def getStreams(): java.util.Set[String] = {
    sinks.keySet()
  }

}
