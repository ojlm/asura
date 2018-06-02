package asura.core.job

import asura.common.model.FormSelect
import org.quartz.Trigger.TriggerState

object TriggerStates {

  val STATES = List(
    FormSelect(label = TriggerState.NONE.name, value = TriggerState.NONE.name),
    FormSelect(label = TriggerState.NORMAL.name, value = TriggerState.NORMAL.name),
    FormSelect(label = TriggerState.PAUSED.name, value = TriggerState.PAUSED.name),
    FormSelect(label = TriggerState.COMPLETE.name, value = TriggerState.COMPLETE.name),
    FormSelect(label = TriggerState.ERROR.name, value = TriggerState.ERROR.name),
    FormSelect(label = TriggerState.BLOCKED.name, value = TriggerState.BLOCKED.name)
  )
}
