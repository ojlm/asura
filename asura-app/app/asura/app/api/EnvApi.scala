package asura.app.api

import asura.app.AppErrorMessages
import asura.common.model.{ApiRes, ApiResError}
import asura.core.ErrorMessages
import asura.core.auth.AuthManager
import asura.core.es.EsResponse
import asura.core.es.model.Environment
import asura.core.es.model.Permissions.Functions
import asura.core.es.service.{EnvironmentService, HttpRequestService, JobService, ScenarioService}
import asura.core.model.QueryEnv
import asura.core.security.PermissionAuthProvider
import asura.play.api.BaseApi.OkApiRes
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnvApi @Inject()(
                        implicit exec: ExecutionContext,
                        val controllerComponents: SecurityComponents,
                        val permissionAuthProvider: PermissionAuthProvider,
                      ) extends BaseApi {

  def getById(group: String, project: String, id: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      EnvironmentService.getEnvById(id).map(env => {
        if (null != env.auth && env.auth.nonEmpty) {
          env.auth.foreach(authData => {
            AuthManager(authData.`type`).foreach(_.mask(authData))
          })
        }
        env
      }).toOkResult
    }
  }

  def put(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { user =>
      val doc = req.bodyAs(classOf[Environment])
      doc.group = group
      doc.project = project
      doc.fillCommonFields(user)
      EnvironmentService.index(doc).toOkResult
    }
  }

  def update(group: String, project: String, id: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_EDIT) { _ =>
      val doc = req.bodyAs(classOf[Environment])
      doc.group = group
      doc.project = project
      EnvironmentService.updateEnv(id, doc).toOkResult
    }
  }

  def query(group: String, project: String) = Action(parse.byteString).async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_LIST) { _ =>
      val q = req.bodyAs(classOf[QueryEnv])
      EnvironmentService.queryEnv(q).toOkResultByEsList()
    }
  }

  def getAllAuth(group: String, project: String) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_VIEW) { _ =>
      Future.successful(OkApiRes(ApiRes(data = AuthManager.getAll())))
    }
  }

  def delete(group: String, project: String, id: String, preview: Option[Boolean]) = Action.async { implicit req =>
    checkPermission(group, Some(project), Functions.PROJECT_COMPONENT_REMOVE) { _ =>
      val ids = Seq(id)
      val res = for {
        c <- HttpRequestService.containEnv(ids)
        s <- ScenarioService.containEnv(ids)
        j <- JobService.containEnv(ids)
      } yield (c, s, j)
      res.flatMap(resTriple => {
        val (caseRes, scenarioRes, jobRes) = resTriple
        if (caseRes.isSuccess && scenarioRes.isSuccess && jobRes.isSuccess) {
          if (preview.nonEmpty && preview.get) {
            Future.successful(toActionResultFromAny(Map(
              "case" -> EsResponse.toApiData(caseRes.result),
              "scenario" -> EsResponse.toApiData(scenarioRes.result),
              "job" -> EsResponse.toApiData(jobRes.result)
            )))
          } else {
            if (caseRes.result.isEmpty && scenarioRes.result.isEmpty && jobRes.result.isEmpty) {
              EnvironmentService.deleteDoc(id).toOkResult
            } else {
              Future.successful(OkApiRes(ApiResError(getI18nMessage(AppErrorMessages.error_CantDeleteEnv))))
            }
          }
        } else {
          val errorRes = if (!scenarioRes.isSuccess) scenarioRes else jobRes
          ErrorMessages.error_EsRequestFail(errorRes).toFutureFail
        }
      })
    }
  }
}
