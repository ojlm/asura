package asura.core.job

import akka.actor.ActorRef

case class JobReportItemStoreDataHelper(
                                         reportId: String,
                                         infix: String,
                                         actorRef: ActorRef,
                                         jobId: String
                                       ) {
  var jobLoopCount: Int = 0

  /**
    * id = `${reportId}_s${stepIndex}_${scenarioIndex}_${jobLoopCount}_{scenarioLoopCount}`
    */
  def generateItemId(scenarioIdx: Int, scenarioLoopCount: Int): String = {
    s"${reportId}_${infix}_${scenarioIdx}_${jobLoopCount}_${scenarioLoopCount}"
  }
}
