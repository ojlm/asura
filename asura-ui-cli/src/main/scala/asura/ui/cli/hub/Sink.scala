package asura.ui.cli.hub

trait Sink[T] {

  def active(params: Object): Unit = {}

  def write(frame: T): Boolean

  def close(): Unit = {}

}
