package asura.ui.cli.args

import picocli.CommandLine.Option

abstract class ServerCommonOptions extends BaseCommand {

  @Option(
    names = Array("-p", "--server-port"),
    arity = "1",
    paramLabel = "port",
    descriptionKey = "server.port",
  )
  var serverPort: Int = 8080

}
