package asura.ui.cli

import java.io.File

import asura.ui.cli.args.{BaseCommand, ConfigCommand, MainCommand}
import ch.qos.logback.classic.Level
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.{ParameterException, ParseResult, UnmatchedArgumentException}

object Main {

  def main(args: Array[String]): Unit = {
    val command = new MainCommand()
    val cmd = new CommandLine(command)
    try {
      val parsed = cmd.parseArgs(args: _*)
      parseVerbosity(parsed)
      if (CommandLine.executeHelpRequest(parsed) == null) {
        if (parsed.hasMatchedOption("c")) {
          val configFile = parsed.matchedOption("c").getValue[File]
          new ConfigCommand(configFile).call()
        } else if (parsed.hasSubcommand) {
          val subParsed = parsed.subcommand()
          parseVerbosity(subParsed)
          val result = subParsed.commandSpec().userObject().asInstanceOf[BaseCommand].call()
          cmd.setExecutionResult(result)
          cmd.getCommandSpec.exitCodeOnSuccess()
        } else {
          cmd.usage(cmd.getOut)
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
