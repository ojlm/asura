package asura.ui.driver

trait UiDriverProvider {

  def getDrivers(): Seq[UiDriverAddress]
}

