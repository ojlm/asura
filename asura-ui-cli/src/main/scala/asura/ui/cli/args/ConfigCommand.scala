package asura.ui.cli.args

import java.io.File
import java.util.concurrent.Callable

import com.typesafe.scalalogging.Logger

class ConfigCommand(config: File) extends Callable[Int] {

  val logger = Logger(classOf[ConfigCommand])

  override def call(): Int = {
    logger.info(s"parse config: ${config.getAbsolutePath}")
    0
  }

}
