package asura.app.api

import akka.actor.ActorSystem
import asura.core.cs.model.AggsQuery
import asura.core.es.service._
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class DomainOnlineLogApi @Inject()(implicit system: ActorSystem,
                                   val exec: ExecutionContext,
                                   val controllerComponents: SecurityComponents
                                  ) extends BaseApi {

  def aggTerms() = Action(parse.byteString).async { implicit req =>
    val aggs = req.bodyAs(classOf[AggsQuery])
    DomainOnlineLogService.aggTerms(aggs).toOkResult
  }
}
