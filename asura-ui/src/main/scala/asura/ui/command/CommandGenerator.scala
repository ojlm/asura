package asura.ui.command

trait CommandGenerator {

  /** block */
  def init(): Unit

  /** block */
  def generate(): Unit

}
