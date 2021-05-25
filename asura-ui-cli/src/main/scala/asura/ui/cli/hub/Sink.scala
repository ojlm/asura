package asura.ui.cli.hub

trait Sink[T] {

  def write(frame: T): Boolean

  def close(): Unit = {}

}
