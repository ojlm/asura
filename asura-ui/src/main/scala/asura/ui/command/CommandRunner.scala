package asura.ui.command

import asura.ui.driver.{CommandMeta, DriverCommandEnd}

trait CommandRunner {

  /** runtime status data */
  val meta: CommandMeta

  /** block */
  def run(): DriverCommandEnd

}
