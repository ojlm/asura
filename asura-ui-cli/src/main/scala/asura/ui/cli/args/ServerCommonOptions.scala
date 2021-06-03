package asura.ui.cli.args

import picocli.CommandLine.Option

abstract class ServerCommonOptions extends BaseCommand {

  @Option(
    names = Array("-p", "--server-port"),
    arity = "1",
    paramLabel = "PORT",
    description = Array("Local server port. Default: 8080.")
  )
  var serverPort: Int = 8080

}
