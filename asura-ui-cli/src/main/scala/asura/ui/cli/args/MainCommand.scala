package asura.ui.cli.args

import java.io.File

import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  header = Array("@|bold,cyan indigo 0.8.0 |@"),
  name = "indigo",
  description = Array("Hold local chrome, adb, electron..."),
  subcommands = Array(
    classOf[ChromeCommand],
    classOf[ElectronCommand],
    classOf[MonkeyCommand],
    classOf[AndroidCommand],
  ),
  footer = Array(
    "",
    "At least config file or 1 subcommand found.",
    "",
  )
)
class MainCommand extends BaseCommand {

  @Option(
    names = Array("-c", "--config"),
    arity = "1",
    paramLabel = "FILE",
    description = Array(
      "Config file.",
      "If this option is provided, the command will be ignored."
    )
  )
  val configFile: File = null

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    throw new RuntimeException("Not here.")
  }

}
