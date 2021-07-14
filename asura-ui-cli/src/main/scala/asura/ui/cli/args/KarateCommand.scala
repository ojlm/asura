package asura.ui.cli.args

import java.io.File
import java.util.Collections

import asura.ui.cli.runner.KarateRunner
import com.intuit.karate.FileUtils
import picocli.CommandLine.{Command, Mixin, Option, Parameters}

@Command(name = "karate")
class KarateCommand extends BaseCommand {

  @Parameters(
    paramLabel = "path",
  )
  var paths: java.util.List[String] = Collections.singletonList(".")

  @Option(
    names = Array("-t", "--tags"),
    paramLabel = "tag",
  )
  var tags: java.util.List[String] = null

  @Option(
    names = Array("-T", "--threads"),
  )
  var threads: Int = 1

  @Option(
    names = Array("-o", "--output"),
  )
  var output: String = FileUtils.getBuildDir()

  @Option(
    names = Array("-f", "--format"),
    split = ",",
    paramLabel = "format",
  )
  var formats: java.util.List[String] = null

  @Option(
    names = Array("-n", "--name"),
    paramLabel = "name",
  )
  var name: String = null

  @Option(
    names = Array("-e", "--env"),
    paramLabel = "env",
  )
  var env: String = null

  @Option(
    names = Array("-w", "--workdir"),
    paramLabel = "dir",
  )
  var workingDir: File = FileUtils.WORKING_DIR

  @Option(
    names = Array("-g", "--configdir"),
    paramLabel = "dir",
  )
  var configDir: String = null

  @Option(
    names = Array("-C", "--clean"),
  )
  var clean: Boolean = false

  @Option(
    names = Array("-D", "--dryrun"),
  )
  var dryRun: Boolean = false

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    KarateRunner.run(this)
    0
  }

}
