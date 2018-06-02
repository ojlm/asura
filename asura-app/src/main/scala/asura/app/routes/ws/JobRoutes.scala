package asura.app.routes.ws

import akka.http.scaladsl.server.Directives._
import asura.GlobalImplicits._
import asura.app.routes.Directives.asuraUser
import asura.core.actor.flow.WebSocketMessageHandler
import asura.core.job.actor.JobStatusActor.JobQueryMessage
import asura.core.job.actor.JobStatusMonitorActor.JobStatusSubscribeMessage
import asura.core.job.actor.JobTestActor.JobTestMessage
import asura.core.job.actor.{JobManualActor, JobStatusActor, JobTestActor, SchedulerActor}

object JobRoutes {

  val jobWsRoutes =
    pathPrefix("job") {
      path("list") {
        val statusActor = system.actorOf(JobStatusActor.props())
        SchedulerActor.statusMonitor ! JobStatusSubscribeMessage(statusActor)
        val handler = WebSocketMessageHandler.newHandleFlow(statusActor, classOf[JobQueryMessage])
        handleWebSocketMessages(handler)
      } ~
        path("test") {
          asuraUser() { username =>
            val testActor = system.actorOf(JobTestActor.props(username))
            val handler = WebSocketMessageHandler.newHandleFlow(testActor, classOf[JobTestMessage])
            handleWebSocketMessages(handler)
          }
        } ~
        pathPrefix("manual") {
          path(Segment) { jobId =>
            asuraUser() { username =>
              val workActor = system.actorOf(JobManualActor.props(jobId, username))
              val handler = WebSocketMessageHandler.newHandleStringFlow(workActor, classOf[String])
              handleWebSocketMessages(handler)
            }
          }
        }
    }
}
