package asura.ui.cli

import java.io.File

import asura.ui.cli.args.{BaseCommand, MainCommand}
import asura.ui.cli.runner.IndigoRunner
import ch.qos.logback.classic.Level
import com.intuit.karate.driver.DriverOptions
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.{ParameterException, ParseResult, UnmatchedArgumentException}

object Main {

  def main(args: Array[String]): Unit = {
    val command = new MainCommand()
    val cmd = new CommandLine(command)
    try {
      DriverOptions.setDriverProvider(DriverProviders.INSTANCE)
      val parsed = cmd.parseArgs(args: _*)
      parseVerbosity(parsed)
      if (CommandLine.executeHelpRequest(parsed) == null) {
        if (parsed.hasMatchedOption("c")) {
          printLogo(cmd)
          val config = parsed.matchedOption("c").getValue[File]
          IndigoRunner.run(config)
        } else if (parsed.hasSubcommand) {
          val subParsed = parsed.subcommand()
          parseVerbosity(subParsed)
          val result = subParsed.commandSpec().userObject().asInstanceOf[BaseCommand].call()
          cmd.setExecutionResult(result)
          cmd.getCommandSpec.exitCodeOnSuccess()
        } else {
          printLogo(cmd)
          IndigoRunner.run(null)
        }
      }
    } catch {
      case t: ParameterException =>
        cmd.getErr.println(t.getMessage)
        if (!UnmatchedArgumentException.printSuggestions(t, cmd.getErr)) t.getCommandLine.usage(cmd.getErr)
      case t: Exception =>
        t.printStackTrace(cmd.getErr)
    }
  }

  def printLogo(cmd: CommandLine): Unit = {
    val writer = cmd.getOut
    val renderer = cmd.getHelpSectionMap.get("header")
    if (writer != null && renderer != null) {
      val logo = renderer.render(cmd.getHelp)
      writer.print(logo)
    }
    writer.flush()
  }

  def parseVerbosity(parsed: ParseResult): Unit = {
    if (parsed.hasMatchedOption("v")) {
      val verbosity = parsed.matchedOption("v").getValue[Array[Boolean]]
      verbosity.length match {
        case 0 => setLoggingLevel(Level.WARN)
        case 1 => setLoggingLevel(Level.INFO)
        case 2 => setLoggingLevel(Level.DEBUG)
        case _ => setLoggingLevel(Level.TRACE)
      }
    }
  }

  def setLoggingLevel(level: Level): Unit = {
    val root = LoggerFactory.getLogger("ROOT").asInstanceOf[ch.qos.logback.classic.Logger]
    root.setLevel(level)
  }

}
