package asura.pea.api

import akka.actor.ActorSystem
import akka.stream.Materializer
import asura.pea.PeaConfig
import asura.pea.model.PeaMember
import asura.play.api.BaseApi
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeApi @Inject()(
                         implicit val system: ActorSystem,
                         implicit val exec: ExecutionContext,
                         implicit val mat: Materializer,
                         val controllerComponents: SecurityComponents
                       ) extends BaseApi {

  def index() = Action.async { implicit req =>
    Future.successful(Ok("asura-pea"))
  }

  def members() = Action.async { implicit req =>
    val children = PeaConfig.zkClient.getChildren.forPath(PeaConfig.zkPath)
    Future.successful(children.asScala.map(PeaMember(_)).filter(m => null != m)).toOkResult
  }
}
