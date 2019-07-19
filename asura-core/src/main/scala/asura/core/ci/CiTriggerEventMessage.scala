package asura.core.ci

import asura.common.util.StringUtils

case class CiTriggerEventMessage(
                                  group: String,
                                  project: String,
                                  env: String,
                                  author: String,
                                  service: String,
                                  `type`: String,
                                  timestamp: Long,
                                ) {

  def eventKey = {
    val sb = StringBuilder.newBuilder
    sb.append(group).append(":").append(project)
    if (StringUtils.isNotEmpty(service)) sb.append(":").append(service)
    if (StringUtils.isNotEmpty(env)) sb.append(":").append(env)
    sb.toString()
  }
}
