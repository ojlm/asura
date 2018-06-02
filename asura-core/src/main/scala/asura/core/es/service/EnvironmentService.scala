package asura.core.es.service

import asura.common.exceptions.{IllegalRequestException, RequestFailException}
import asura.common.model.{ApiMsg, BoolErrorRes}
import asura.common.util.{FutureUtils, StringUtils}
import asura.core.es.model.{Environment, FieldKeys}
import asura.core.es.{EsClient, EsConfig}
import asura.core.util.JacksonSupport
import asura.core.util.JacksonSupport.jacksonJsonIndexable
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

object EnvironmentService {

  val logger = Logger("EnvironmentService")

  def index(env: Environment) = {
    val (isOk, errMsg) = validate(env)
    if (!isOk) {
      FutureUtils.illegalArgs(errMsg)
    } else {
      EsClient.httpClient.execute {
        indexInto(Environment.Index / EsConfig.DefaultType).doc(env).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def deleteDoc(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        delete(id).from(Environment.Index / EsConfig.DefaultType).refresh(RefreshPolicy.WAIT_UNTIL)
      }
    }
  }

  def getById(id: String) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs(ApiMsg.INVALID_REQUEST_BODY)
    } else {
      EsClient.httpClient.execute {
        search(Environment.Index).query(idsQuery(id))
      }
    }
  }

  def getAll(project: String) = {
    EsClient.httpClient.execute {
      search(Environment.Index)
        .query(termQuery(FieldKeys.FIELD_PROJECT, project))
        .limit(EsConfig.MaxCount)
        .sortByFieldDesc(FieldKeys.FIELD_CREATED_AT)
    }
  }

  def updateEnv(id: String, env: Environment) = {
    if (StringUtils.isEmpty(id)) {
      FutureUtils.illegalArgs("Empty id")
    } else {
      val (isOk, errMsg) = validate(env)
      if (!isOk) {
        FutureUtils.illegalArgs(errMsg)
      } else {
        EsClient.httpClient.execute {
          update(id).in(Environment.Index / EsConfig.DefaultType)
            .doc(JacksonSupport.stringify(env.toUpdateMap))
            .refresh(RefreshPolicy.WAIT_UNTIL)
        }
      }
    }
  }

  def validate(env: Environment): BoolErrorRes = {
    if (StringUtils.isEmpty(env.summary)) {
      (false, "Empty summary")
    } else if (StringUtils.isEmpty(env.group)) {
      (false, "Empty group")
    } else if (StringUtils.isEmpty(env.project)) {
      (false, "Empty project")
    } else if (Option(env.port).isDefined && (env.port < 0 || env.port > 65535)) {
      (false, "Illegal port")
    } else {
      (true, null)
    }
  }

  def getEnvById(id: String)(implicit executor: ExecutionContext): Future[Environment] = {
    if (StringUtils.isEmpty(id)) {
      Future.successful(null)
    } else {
      getById(id).map(res => {
        res match {
          case Right(success) =>
            if (success.result.isEmpty) {
              throw IllegalRequestException(s"Env: ${id} not found.")
            } else {
              val hit = success.result.hits.hits(0)
              JacksonSupport.parse(hit.sourceAsString, classOf[Environment])
            }
          case Left(failure) =>
            throw RequestFailException(failure.error.reason)
        }
      })
    }
  }
}
