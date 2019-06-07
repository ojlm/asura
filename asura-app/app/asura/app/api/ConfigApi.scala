package asura.app.api

import akka.actor.ActorSystem
import asura.common.model.ApiRes
import asura.core.assertion.Assertions
import asura.core.script.function.Functions
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class ConfigApi @Inject()(implicit system: ActorSystem,
                          val exec: ExecutionContext,
                          val controllerComponents: SecurityComponents
                         ) extends BaseApi {

  def getBasics() = Action {
    OkApiRes(ApiRes(data = Map(
      "assertions" -> Assertions.getAll(),
      "transforms" -> Functions.getAllTransforms(),
    )))
  }

  def getAllAssertions() = Action {
    OkApiRes(ApiRes(data = Assertions.getAll()))
  }

  def getAllTransforms() = Action {
    OkApiRes(ApiRes(data = Functions.getAllTransforms()))
  }
}
