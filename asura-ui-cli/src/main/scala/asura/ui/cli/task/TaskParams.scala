package asura.ui.cli.task

import java.util.Collections

import asura.common.util.{JsonUtils, StringUtils}
import asura.ui.cli.task.TaskParams.{KarateParams, TaskType}

case class TaskParams(
                       `type`: Int,
                       params: Object,
                     ) {

  def karate(): KarateParams = {
    if (TaskType.KARATE == `type`) {
      if (params != null) {
        JsonUtils.mapper.convertValue(params, classOf[KarateParams])
      } else {
        KarateParams()
      }
    } else {
      throw new RuntimeException(s"type: ${`type`} is not a karate")
    }
  }

}

object TaskParams {

  object TaskType {
    val STOP = 0
    val KARATE = 1
  }

  case class KarateParams(
                           clean: Boolean = false,
                           output: String = "target",
                           name: String = null,
                           env: String = null,
                           workingDir: String = StringUtils.EMPTY,
                           configDir: String = null,
                           paths: java.util.List[String] = Collections.singletonList("."),
                           tags: java.util.List[String] = null,
                           formats: java.util.List[String] = null,
                         )

}
