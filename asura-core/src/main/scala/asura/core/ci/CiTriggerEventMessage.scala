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
    val sb = new StringBuilder()
    sb.append(group).append(":").append(project).append(":")
    if (StringUtils.isNotEmpty(env)) sb.append(env)
    sb.append(":")
    if (StringUtils.isNotEmpty(service)) sb.append(service)
    sb.toString()
  }
}
