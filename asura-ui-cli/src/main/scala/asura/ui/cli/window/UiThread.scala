package asura.ui.cli.window

import javafx.application.Platform

object UiThread {

  def run(runnable: => Unit): Unit = {
    Platform.runLater(() => runnable)
  }

  def run(runnable: Runnable): Unit = {
    Platform.runLater(runnable)
  }

}
