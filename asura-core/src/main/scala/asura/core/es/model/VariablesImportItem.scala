package asura.core.es.model

/**
  *
  * @param name        variable name, this should be unique in the whole scope
  * @param scope       [[asura.core.runtime.RuntimeContext.KEY__G]],
  *                    [[asura.core.runtime.RuntimeContext.KEY__J]],
  *                    [[asura.core.runtime.RuntimeContext.KEY__S]],
  * @param value       the value
  * @param description some description
  * @param enabled     enabled
  * @param function    transform function
  */
case class VariablesImportItem(
                                name: String,
                                scope: String,
                                value: String,
                                description: String,
                                enabled: Boolean = true,
                                function: String = null,
                              )
