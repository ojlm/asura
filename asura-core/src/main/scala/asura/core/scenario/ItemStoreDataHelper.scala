package asura.core.scenario

import akka.actor.ActorRef

/**
  * id = `${reportId}_${infix}_${array_index}`
  */
case class ItemStoreDataHelper(reportId: String, infix: String, actorRef: ActorRef, jobId: String)
