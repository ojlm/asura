package asura.ui.cli.args

import picocli.CommandLine.Option

abstract class SubBaseCommand extends BaseCommand {

  @Option(
    names = Array("--enable-server"),
    description = Array(
      "Start a local server. Default: false.",
    )
  )
  var enableServer: Boolean = false

  @Option(
    names = Array("-p", "--server-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Local server port. Default: 8080.")
  )
  var serverPort: Int = 8080

}
