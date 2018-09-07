package asura.core.util

import asura.common.exceptions.InvalidStatusException
import asura.common.util.{LogUtils, StringUtils}
import com.typesafe.scalalogging.Logger
import jodd.util.StringTemplateParser

import scala.collection.{immutable, mutable}

object StringTemplate {

  /** wrapped by "{{" and "}}" */
  val mustacheParser = StringTemplateParser.create()
    .setMacroPrefix(null)
    .setMacroStart("{{")
    .setMacroEnd("}}")

  /** wrapped by "${" and "}" */
  val templateLiteralsParser = StringTemplateParser.create()
    .setMacroPrefix(null)
    .setMacroStart("${")
    .setMacroEnd("}")
  val logger = Logger("StringTemplate")

  val uriPathParser = StringTemplateParser.create()
    .setMacroPrefix(null)
    .setMacroStart("{")
    .setMacroEnd("}")

  /** return macro if path not found */
  def parseStringWithJsonPath(tpl: String, json: String): String = {
    templateLiteralsParser.parse(tpl, macroName => {
      try {
        JsonPathUtils.read[String](json, macroName)
      } catch {
        case t: Throwable =>
          logger.warn(LogUtils.stackTraceToString(t))
          "${" + macroName + "}"
      }
    })
  }

  /** return macro if path not found */
  def parseMapWithJsonPath(tpl: String, map: java.util.Map[String, Any]): String = {
    templateLiteralsParser.parse(tpl, macroName => {
      try {
        JsonPathUtils.read[String](map, macroName)
      } catch {
        case t: Throwable =>
          logger.warn(LogUtils.stackTraceToString(t))
          "${" + macroName + "}"
      }
    })
  }

  /** return `empty string` if key not found in `context` */
  def parse(tpl: String, context: mutable.Map[String, String]): String = {
    templateLiteralsParser.parse(tpl, macroName => {
      context.get(macroName) match {
        case None => StringUtils.EMPTY
        case Some(value) => value
      }
    })
  }

  /** return `empty string` if key not found in `context` */
  def parse(tpl: String, context: immutable.Map[String, String]): String = {
    templateLiteralsParser.parse(tpl, macroName => {
      context.get(macroName) match {
        case None => StringUtils.EMPTY
        case Some(value) => value
      }
    })
  }

  /**
    * parse uri path template which use `{` and `}` as macro,
    * will throw `asura.common.exceptions.InvalidStatusException` if value not found
    */
  def uriPathParse(tpl: String, context: immutable.Map[String, String]): String = {
    uriPathParser.parse(tpl, macroName => {
      context.get(macroName) match {
        case None => throw InvalidStatusException(s"${macroName}: path template variable not found")
        case Some(value) => value
      }
    })
  }
}
