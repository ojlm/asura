package asura.app.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import asura.app.api.model.TestSql
import asura.common.util.StringUtils
import asura.core.cs.model.QuerySqlRequest
import asura.core.es.actor.ActivitySaveActor
import asura.core.es.model.{Activity, SqlRequest}
import asura.core.es.service.SqlRequestService
import asura.core.sql.actor.SqlRequestInvokerActor
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class SqlApi @Inject()(
                        implicit val system: ActorSystem,
                        val exec: ExecutionContext,
                        val configuration: Configuration,
                        val controllerComponents: SecurityComponents,
                      ) extends BaseApi {

  val activityActor = system.actorOf(ActivitySaveActor.props())
  implicit val timeout: Timeout = 30.seconds
  val sqlInvoker = system.actorOf(SqlRequestInvokerActor.props(), "sql-invoker")

  def test() = Action(parse.byteString).async { implicit req =>
    val testMsg = req.bodyAs(classOf[TestSql])
    val sqlReq = testMsg.request
    val error = SqlRequestService.validate(sqlReq)
    if (null == error) {
      val user = getProfileId()
      activityActor ! Activity(sqlReq.group, sqlReq.project, user, Activity.TYPE_TEST_SQL, StringUtils.notEmptyElse(testMsg.id, StringUtils.EMPTY))
      (sqlInvoker ? sqlReq).toOkResult
    } else {
      error.toFutureFail
    }
  }

  def getById(id: String) = Action.async { implicit req =>
    SqlRequestService.getById(id).toOkResultByEsOneDoc(id)
  }

  def delete(id: String) = Action.async { implicit req =>
    SqlRequestService.deleteDoc(id).toOkResult
  }

  def put() = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[SqlRequest])
    val user = getProfileId()
    doc.fillCommonFields(user)
    SqlRequestService.index(doc).map(res => {
      activityActor ! Activity(doc.group, doc.project, user, Activity.TYPE_NEW_SQL, res.id)
      toActionResultFromAny(res)
    })
  }

  def query() = Action(parse.byteString).async { implicit req =>
    val q = req.bodyAs(classOf[QuerySqlRequest])
    SqlRequestService.query(q).toOkResultByEsList(false)
  }

  def update(id: String) = Action(parse.byteString).async { implicit req =>
    val doc = req.bodyAs(classOf[SqlRequest])
    SqlRequestService.updateDoc(id, doc).toOkResult
  }
}
