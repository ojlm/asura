package asura.core.es.model

import asura.core.es.model.ScenarioStep.StepData

case class ScenarioStep(
                         id: String,
                         `type`: String,
                         stored: Boolean = false, // do need to store in context
                         enabled: Boolean = true,
                         data: StepData = null,
                       )

object ScenarioStep {

  // use `case` for http request backward compatible
  val TYPE_HTTP = "case"
  val TYPE_SQL = "sql"
  val TYPE_DUBBO = "dubbo"
  val TYPE_SCENARIO = "scenario"
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

  case class JumpCondition(assert: Map[String, Any] = null, to: Int = 0)

  case class JumpConditions(conditions: Seq[JumpCondition])

}
