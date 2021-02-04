package asura.ui.cli.args

import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Spec.Target.MIXEE
import picocli.CommandLine.{Option, Spec}

class LoggingMixin {

  @Spec(MIXEE)
  private val mixee: CommandSpec = null
  var verbosity = new Array[Boolean](0)

  /**
   * Sets the specified verbosity on the LoggingMixin of the top-level command.
   *
   * @param verbosity the new verbosity value
   */
  @Option(
    names = Array("-v", "--verbose"),
    description = Array(
      "Specify multiple -v options to increase verbosity.",
      "For example, `-v -v -v` or `-vvv`.",
    ),
  )
  def setVerbose(verbosity: Array[Boolean]): Unit = {
    // Each subcommand that mixes in the LoggingMixin has its own instance
    // of this class, so there may be many LoggingMixin instances.
    // We want to store the verbosity value in a single, central place,
    // so we find the top-level command,
    // and store the verbosity level on our top-level command's LoggingMixin.
    mixee.root.userObject.asInstanceOf[MainCommand].loggingMixin.verbosity = verbosity
  }

}
