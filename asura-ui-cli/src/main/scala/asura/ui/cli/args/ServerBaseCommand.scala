package asura.ui.cli.args

import picocli.CommandLine.Option

abstract class ServerBaseCommand extends ServerCommonOptions {

  @Option(
    names = Array("--enable-server"),
    descriptionKey = "server.enable",
  )
  var enableServer: Boolean = true

}
