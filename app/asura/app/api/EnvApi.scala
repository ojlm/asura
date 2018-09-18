package asura.app.api

import asura.app.api.BaseApi.OkApiRes
import asura.common.model.ApiRes
import asura.core.auth.AuthManager
import asura.core.cs.model.QueryEnv
import asura.core.es.model.Environment
import asura.core.es.service.EnvironmentService
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.ExecutionContext

@Singleton
class EnvApi @Inject()(implicit exec: ExecutionContext, val controllerComponents: SecurityComponents)
  extends BaseApi {

  def getById(id: String) = Action.async { implicit req =>
    EnvironmentService.getById(id).toOkResultByEsOneDoc(id)
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    val env = req.bodyAs(classOf[Environment])
    env.fillCommonFields(getProfileId())
    EnvironmentService.index(env).toOkResult
  }

  def update(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    val env = req.bodyAs(classOf[Environment])
    env.fillCommonFields(getProfileId())
    EnvironmentService.updateEnv(id, env).toOkResult
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val queryEnv = req.bodyAs(classOf[QueryEnv])
    EnvironmentService.queryEnv(queryEnv).toOkResultByEsList()
  }

  def getAllAuth() = Action {
    OkApiRes(ApiRes(data = AuthManager.getAll()))
  }
}
