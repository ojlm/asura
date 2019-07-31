package asura.core.es.model

import asura.common.util.StringUtils
import com.fasterxml.jackson.annotation.JsonIgnore

/**
  *
  * @param name        variable name, this should be unique in the whole scope
  * @param scope       [[asura.core.runtime.RuntimeContext.KEY__G]],
  *                    [[asura.core.runtime.RuntimeContext.KEY__J]],
  *                    [[asura.core.runtime.RuntimeContext.KEY__S]],
  * @param value       the value
  * @param description some description
  * @param enabled     enabled
  * @param exposed     does exposed to toptop
  * @param function    transform function
  */
case class VariablesImportItem(
                                name: String,
                                scope: String,
                                value: Object,
                                description: String,
                                `type`: String = null,
                                extra: VariablesItemExtraData = null,
                                enabled: Boolean = true,
                                exposed: Boolean = true,
                                function: String = null,
                              ) {

  @JsonIgnore
  def isValid(): Boolean = {
    !StringUtils.hasEmpty(name, scope) && null != value && enabled
  }
}

object VariablesImportItem {

  val TYPE_ENUM = "enum"
}
