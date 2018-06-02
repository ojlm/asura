package asura.app.routes.api

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Source
import asura.GlobalImplicits.system
import asura.core.actor.flow.WebSocketMessageHandler
import asura.core.actor.messages.SenderMessage
import asura.core.job.actor.JobCiActor

object ApiRoutes {

  val apiRoutes = {
    pathPrefix("api") {
      pathPrefix("v1") {
        path("ci") {
          parameters('id) { id =>
            val ciActor = system.actorOf(JobCiActor.props(id))
            val handler = WebSocketMessageHandler.newHandleStringFlow(ciActor, classOf[String])
            handleWebSocketMessages(handler)
          }
        }
      } ~
        pathPrefix("v2") {
          respondWithHeaders(RawHeader("Cache-Control", "no-cache"), RawHeader("X-Accel-Buffering", "no")) {
            path("ci") {
              parameters('id) { id =>
                complete {
                  val ciActor = system.actorOf(JobCiActor.props(id))
                  Source.actorRef[String](100, OverflowStrategy.dropHead)
                    .mapMaterializedValue(ref => ciActor ! SenderMessage(ref))
                    .map(event => {
                      ServerSentEvent(event)
                    })
                }
              }
            }
          }
        }
    }
  }
}
