package asura.ui.cli.args

import java.util.concurrent.Callable

import picocli.CommandLine.Command

@Command(
  versionProvider = classOf[PropertiesVersionProvider],
  resourceBundle = "i18n.Messages",
  mixinStandardHelpOptions = true,
  sortOptions = false,
)
abstract class BaseCommand extends Callable[Int] {

}
