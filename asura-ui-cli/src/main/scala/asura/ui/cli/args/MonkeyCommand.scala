package asura.ui.cli.args

import java.io.File

import asura.ui.cli.args.MonkeyCommand.MonkeyDriver
import asura.ui.cli.runner.MonkeyRunner
import com.fasterxml.jackson.annotation.JsonIgnore
import com.typesafe.scalalogging.Logger
import picocli.CommandLine.{ArgGroup, Command, Mixin, Option, Parameters}

@Command(name = "monkey")
class MonkeyCommand extends BaseCommand {

  @JsonIgnore
  val logger = Logger(classOf[MonkeyCommand])

  @ArgGroup(exclusive = true, multiplicity = "1")
  val driver: MonkeyDriver = null

  @Parameters(
    arity = "1",
    paramLabel = "path",
  )
  val config: File = null

  @JsonIgnore
  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    MonkeyRunner.run(this)
    0
  }

}

object MonkeyCommand {

  class MonkeyDriver {

    @Option(
      names = Array("--chrome"),
    )
    var chrome: Boolean = false

    @Option(
      names = Array("--electron"),
    )
    var electron: Boolean = false

  }

}
