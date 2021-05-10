package asura.ui.cli.hub

import java.util.concurrent.ConcurrentHashMap

object StreamHub {

  private val sinks = new ConcurrentHashMap[String, ConcurrentHashMap[FrameSink, FrameSink]]()

  def getSinks(name: String): ConcurrentHashMap[FrameSink, FrameSink] = {
    if (sinks.containsKey(name)) {
      sinks.get(name)
    } else {
      val map = new ConcurrentHashMap[FrameSink, FrameSink]()
      val sink = sinks.putIfAbsent(name, map)
      if (sink == null) map else sink
    }
  }

  def enter(name: String, sink: FrameSink): Unit = {
    getSinks(name).put(sink, sink)
  }

  def leave(name: String, sink: FrameSink): Unit = {
    getSinks(name).remove(sink)
  }

  def write(sinks: ConcurrentHashMap[FrameSink, FrameSink], frame: StreamFrame): Unit = {
    sinks.keySet().forEach(sink => sink.write(frame))
  }

  def getStreams(): java.util.Set[String] = {
    sinks.keySet()
  }

}
