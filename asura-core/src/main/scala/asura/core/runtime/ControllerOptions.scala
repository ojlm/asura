package asura.core.runtime

/**
  *
  * @param from         step from
  * @param to           step to
  * @param enableLog    reserved not used
  * @param enableReport reserved not used
  */
case class ControllerOptions(
                              from: Int = -1,
                              to: Int = -1,
                              enableLog: Boolean = true,
                              enableReport: Boolean = true,
                            )
