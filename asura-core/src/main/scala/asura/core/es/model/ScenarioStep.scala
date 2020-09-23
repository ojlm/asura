package asura.core.es.model

import asura.common.util.StringUtils
import asura.core.es.model.ScenarioStep.StepData
import com.fasterxml.jackson.annotation.JsonIgnore

case class ScenarioStep(
                         var id: String,
                         `type`: String,
                         enabled: Boolean = true,
                         data: StepData = null,
                       ) {

  @JsonIgnore
  def isScenarioStep(): Boolean = {
    if (StringUtils.isNotEmpty(id)) {
      StringUtils.isEmpty(`type`) || ScenarioStep.TYPE_SCENARIO.equals(`type`)
    } else {
      false
    }
  }
}

object ScenarioStep {

  // use `case` for http request backward compatible
  val TYPE_HTTP = "case"
  val TYPE_SQL = "sql"
  val TYPE_DUBBO = "dubbo"
  val TYPE_SCENARIO = "scenario"
  val TYPE_JOB = "job"
  val TYPE_DELAY = "delay"
  val TYPE_JUMP = "jump"

  val TIME_UNIT_MILLI = "milli"
  val TIME_UNIT_SECOND = "second"
  val TIME_UNIT_MINUTE = "minute"

  case class StepData(
                       delay: DelayCondition = null,
                       jump: JumpConditions = null,
                     )

  case class DelayCondition(value: Int, timeUnit: String)

  case class AssertJumpCondition(
                                  assert: Map[String, Any] = null,
                                  to: Int = 0,
                                )

  case class JumpConditions(
                             `type`: Int = 0, // 0: assert, 1 : script
                             conditions: Seq[AssertJumpCondition] = Nil,
                             script: String = null, // which will return a integer
                           )

}
