package asura.ui.cli.task

import java.util.concurrent.atomic.AtomicInteger

object TaskThreadGroup {

  private val threadNumber = new AtomicInteger(0)
  private val INSTANCE = new ThreadGroup("tasks")

  def newKarate(runnable: Runnable): Thread = {
    new Thread(INSTANCE, runnable, s"karate-${threadNumber.addAndGet(1)}")
  }

}
