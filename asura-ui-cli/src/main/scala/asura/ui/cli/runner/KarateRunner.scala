package asura.ui.cli.runner

import java.io.File

import asura.ui.cli.args.KarateCommand
import com.intuit.karate.{FileUtils, Runner}
import com.typesafe.scalalogging.Logger

object KarateRunner {

  val logger = Logger("KarateRunner")

  def run(args: KarateCommand): Unit = {
    if (args.clean) {
      FileUtils.deleteDirectory(new File(args.output))
      logger.info(s"deleted directory: ${args.output}")
    }
    val builder = Runner.path(args.paths)
    builder.tags(args.tags)
    builder.scenarioName(args.name)
    builder.karateEnv(args.env)
    builder.workingDir(args.workingDir)
    builder.buildDir(args.output)
    builder.configDir(args.configDir)
    builder.outputHtmlReport(if (args.formats == null) true else !args.formats.contains("~html"))
    builder.outputCucumberJson(if (args.formats == null) false else args.formats.contains("cucumber:json"))
    builder.outputJunitXml(if (args.formats == null) false else args.formats.contains("junit:xml"))
    builder.dryRun(args.dryRun)
    builder.parallel(args.threads)
  }

}
