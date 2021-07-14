package asura.ui.cli.args

import java.io.File

import picocli.CommandLine.{Command, Mixin, Option}

@Command(
  name = "indigo",
  subcommands = Array(
    classOf[KarateCommand],
    classOf[ChromeCommand],
    classOf[ElectronCommand],
    classOf[MonkeyCommand],
    classOf[AndroidCommand],
  ),
)
class MainCommand extends BaseCommand {

  @Option(
    names = Array("-c", "--config"),
    arity = "1",
    paramLabel = "file",
  )
  val configFile: File = null

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    throw new RuntimeException("Not here.")
  }

}
