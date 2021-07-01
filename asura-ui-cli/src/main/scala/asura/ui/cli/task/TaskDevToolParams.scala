package asura.ui.cli.task

import asura.common.util.HostUtils
import asura.ui.driver.DevToolsProtocol

case class TaskDevToolParams(
                              var method: String = null,
                              var level: String = null,
                              var source: String = null,
                              var text: String = null,
                              var data: java.util.Map[Object, Object] = null,
                              var hostname: String = HostUtils.hostname,
                              var timestamp: Long = System.currentTimeMillis(),
                            )

object TaskDevToolParams {

  def apply(data: java.util.Map[String, AnyRef]): TaskDevToolParams = {
    if (data.containsKey(DevToolsProtocol.METHOD)) {
      val log = TaskDevToolParams()
      val method = data.get(DevToolsProtocol.METHOD).asInstanceOf[String]
      val params = data.get("params")
      if (params != null) {
        val paramsMap = params.asInstanceOf[java.util.Map[Object, Object]]
        if (method.equals(DevToolsProtocol.METHOD_Log_entryAdded)) {
          val entry = paramsMap.get("entry")
          if (entry != null) {
            val entryMap = entry.asInstanceOf[java.util.Map[Object, Object]]
            val level = entryMap.get("level")
            if (level != null) {
              log.level = level.asInstanceOf[String]
              entryMap.remove("level")
            }
            val source = entryMap.get("source")
            if (source != null) {
              log.source = source.asInstanceOf[String]
              entryMap.remove("source")
            }
            val text = entryMap.get("text")
            if (null != text) {
              log.text = text.asInstanceOf[String]
              entryMap.remove("text")
            }
            val timestamp = entryMap.get("timestamp")
            if (timestamp != null) {
              log.timestamp = timestampToLong(timestamp)
              entryMap.remove("timestamp")
            }
            log.data = entryMap
          }
        } else {
          if (log.method.equals(DevToolsProtocol.METHOD_Runtime_consoleAPICalled)) {
            val typeObj = paramsMap.get("type")
            if (typeObj != null) {
              log.level = typeObj.asInstanceOf[String]
            }
            val argsObj = paramsMap.get("args")
            if (argsObj != null) {
              val sb = new StringBuilder()
              argsObj.asInstanceOf[java.util.List[java.util.Map[String, Object]]].forEach(arg => {
                val argType = arg.get("type")
                if ("string".equals(argType)) {
                  sb.append(arg.get("value")).append(" ")
                } else if ("number".equals(argType)) {
                  sb.append(arg.get("value").toString()).append(" ")
                }
              })
              if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1)
              log.text = sb.toString()
            }
          }
          val timestamp = paramsMap.get("timestamp")
          if (timestamp != null) {
            log.timestamp = timestampToLong(timestamp)
            paramsMap.remove("timestamp")
          }
          log.data = paramsMap
        }
      }
      log
    } else {
      null
    }
  }

  private def timestampToLong(timestamp: Object): Long = {
    if (timestamp.isInstanceOf[java.math.BigDecimal]) {
      timestamp.asInstanceOf[java.math.BigDecimal].longValue()
    } else if (timestamp.isInstanceOf[java.math.BigInteger]) {
      timestamp.asInstanceOf[java.math.BigInteger].longValue()
    } else if (timestamp.isInstanceOf[java.lang.Long]) {
      timestamp.asInstanceOf[java.lang.Long]
    } else if (timestamp.isInstanceOf[BigDecimal]) {
      timestamp.asInstanceOf[BigDecimal].toLong
    } else if (timestamp.isInstanceOf[Long]) {
      timestamp.asInstanceOf[Long]
    } else {
      0L
    }
  }

}
