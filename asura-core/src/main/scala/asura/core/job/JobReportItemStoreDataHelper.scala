package asura.core.job

import akka.actor.ActorRef

/**
  * id = `${reportId}_${infix}_${array_index}`
  */
case class JobReportItemStoreDataHelper(reportId: String, infix: String, actorRef: ActorRef, jobId: String)
