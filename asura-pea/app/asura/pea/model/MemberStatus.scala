package asura.pea.model

import asura.common.util.StringUtils
import asura.pea.actor.PeaWorkerActor

/** node data
  *
  * @param status [[asura.pea.actor.PeaWorkerActor.NODE_STATUS_IDLE]] or [[asura.pea.actor.PeaWorkerActor.NODE_STATUS_RUNNING]]
  * @param runId  report id of last job
  * @param start  start time of last job
  * @param end    end time of last job
  * @param code   code of last job
  * @param errMsg error message of last job
  */
case class MemberStatus(
                         var status: String = PeaWorkerActor.NODE_STATUS_IDLE,
                         var runId: String = StringUtils.EMPTY,
                         var start: Long = 0L,
                         var end: Long = 0L,
                         var code: Int = 0,
                         var errMsg: String = null,
                       )
