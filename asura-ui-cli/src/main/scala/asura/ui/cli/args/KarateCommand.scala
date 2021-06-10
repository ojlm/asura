package asura.ui.cli.args

import java.io.File
import java.util.Collections

import asura.ui.cli.runner.KarateRunner
import com.intuit.karate.FileUtils
import picocli.CommandLine.{Command, Mixin, Option, Parameters}

@Command(
  header = Array("@|cyan Run karate features |@"),
  name = "karate",
  description = Array("Run karate features"),
)
class KarateCommand extends BaseCommand {

  @Parameters(
    paramLabel = "path",
    description = Array(
      "One or more tests (features) or search-paths to run.",
      "Default search the working directory.",
    )
  )
  var paths: java.util.List[String] = Collections.singletonList(".")

  @Option(
    names = Array("-t", "--tags"),
    paramLabel = "tag",
    description = Array("Cucumber tags - e.g. '@smoke,~@ignore'.")
  )
  var tags: java.util.List[String] = null

  @Option(
    names = Array("-T", "--threads"),
    description = Array("Number of threads when running tests.")
  )
  var threads: Int = 1

  @Option(
    names = Array("-o", "--output"),
    description = Array("Directory where logs and reports are output. Default: 'target'.")
  )
  var output: String = FileUtils.getBuildDir()

  @Option(
    names = Array("-f", "--format"),
    split = ",",
    paramLabel = "format",
    description = Array(
      "Comma separate report output formats.",
      "Tilde excludes the output report.",
      "Html report is included by default unless it's negated.",
      "e.g. '-f json,cucumber:json,junit:xml. ",
      "Possible values [html: Karate HTML, json: Karate JSON, cucumber:json: Cucumber JSON, junit:xml: JUnit XML]"
    )
  )
  var formats: java.util.List[String] = null

  @Option(
    names = Array("-n", "--name"),
    paramLabel = "name",
    description = Array("Scenario name.")
  )
  var name: String = null

  @Option(
    names = Array("-e", "--env"),
    paramLabel = "env",
    description = Array("Value of 'karate.env'.")
  )
  var env: String = null

  @Option(
    names = Array("-w", "--workdir"),
    paramLabel = "dir",
    description = Array("Working directory, defaults to '.'.")
  )
  var workingDir: File = FileUtils.WORKING_DIR

  @Option(
    names = Array("-g", "--configdir"),
    paramLabel = "dir",
    description = Array("Directory where 'karate-config.js' is expected (default 'classpath:' or <workingdir>)")
  )
  var configDir: String = null

  @Option(
    names = Array("-C", "--clean"),
    description = Array("Clean output directory.")
  )
  var clean: Boolean = false

  @Option(
    names = Array("-D", "--dryrun"),
    description = Array("Dry run, generate html reports only.")
  )
  var dryRun: Boolean = false

  @Mixin
  val loggingMixin: LoggingMixin = null

  override def call(): Int = {
    KarateRunner.run(this)
    0
  }

}
