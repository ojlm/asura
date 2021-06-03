package asura.ui.cli.args

import picocli.CommandLine.Option

abstract class ServerBaseCommand extends ServerCommonOptions {

  @Option(
    names = Array("--enable-server"),
    description = Array(
      "Start a local server. Default: false.",
    )
  )
  var enableServer: Boolean = false

}
