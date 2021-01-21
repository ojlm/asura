package asura.ui.driver

object DevToolsProtocol {

  val METHOD = "method"
  val METHOD_Log_entryAdded = "Log.entryAdded"
  val METHOD_Runtime_ = "Runtime."
  val METHOD_Runtime_consoleAPICalled = "Runtime.consoleAPICalled"

  def isNeedLog(method: Any): Boolean = {
    if (method == null) {
      false
    } else {
      METHOD_Log_entryAdded.equals(method) || method.toString.startsWith(METHOD_Runtime_)
    }
  }
}
