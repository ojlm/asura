package asura.ui.cli.hub

trait FrameSink {

  def write(frame: StreamFrame): Boolean

  def close(): Unit

}
