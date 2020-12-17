package asura.ui.command

import asura.ui.driver.DriverCommandEnd

trait CommandRunner {

  /** block */
  def run(): DriverCommandEnd

}
