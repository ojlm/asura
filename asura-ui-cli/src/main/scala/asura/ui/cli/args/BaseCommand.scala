package asura.ui.cli.args

import java.util.concurrent.Callable

import picocli.CommandLine.Command

@Command(
  version = Array("indigo 0.8.0"),
  mixinStandardHelpOptions = true,
  sortOptions = false,
  synopsisHeading = "%n@|bold Usage|@:%n",
  descriptionHeading = "%n@|bold Description|@:%n",
  optionListHeading = "@|bold %nOptions|@:%n",
  synopsisSubcommandLabel = "[command]",
  commandListHeading = "@|bold %nCommands|@:%n",
  footer = Array(
    "",
  )
)
abstract class BaseCommand extends Callable[Int] {

}
