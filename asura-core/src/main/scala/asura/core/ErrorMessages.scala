package asura.core

import com.sksamuel.elastic4s.http.Response

import scala.concurrent.Future

object ErrorMessages extends Enumeration {

  // message enumeration
  def error_EsRequestFail(response: Response[_]) = Val(response.error.reason)

  val error_IllegalGroupId = Val("Illegal group id")
  val error_GroupExists = Val("Group already exists")
  val error_IdNonExists = Val("Id non exists")
  val error_GroupIdEmpty = Val("Group id must not be empty")
  val error_IllegalProjectId = Val("Illegal project id")
  val error_ProjectExists = Val("Project already exists")
  val error_EmptyRequestBody = Val("Empty request body")
  val error_EmptyGroup = Val("Empty group")
  val error_EmptyProject = Val("Empty project")
  val error_EmptyPath = Val("Empty path")
  val error_EmptyMethod = Val("Empty method")
  val error_EmptyCase = Val("Empty case")
  val error_EmptySummary = Val("Empty summary")
  val error_EmptyRequest = Val("Empty request body")
  val error_EmptyNamespace = Val("Empty namespace")
  val error_EmptyId = Val("Empty id")
  val error_IndexSuccess = Val("Create success")
  val error_UpdateSuccess = Val("Update success")
  val error_ServerError = Val("Server Error")
  val error_EmptyEnv = Val("Empty env")
  val error_EmptyScenario = Val("Empty scenario")
  val error_EmptyJobName = Val("Empty job name")
  val error_EmptyJobType = Val("Empty job type")
  val error_EmptyScheduler = Val("Empty scheduler")
  val error_InvalidToken = Val("Invalid token")

  def error_NoSchedulerDefined(scheduler: String) = Val(scheduler)

  def error_NoJobDefined(job: String) = Val(job)

  def error_JobValidate(msg: String) = Val(msg)

  def error_NoNotifyImplementation(`type`: String) = Val(`type`)

  def error_ReportNotifyError(msg: String) = Val(msg)

  def error_NotRegisteredAuth(authType: String) = Val(authType)

  def error_DuplicateApi(msg: String) = Val(s"Duplicate api: $msg")

  def error_Throwable(t: Throwable) = new Val(t)

  def error_IdsNotFound(ids: Seq[String]) = Val(ids.mkString(","))

  case class Val(val errMsg: String, val t: Throwable = null) extends super.Val {

    def this(t: Throwable) {
      this(t.getMessage, t)
    }

    def toException: ErrorMessageException = {
      ErrorMessageException(this)
    }

    def toFutureFail: Future[Nothing] = {
      Future.failed(ErrorMessageException(this))
    }

    lazy val name = this.toString
  }

  case class ErrorMessageException(error: ErrorMessages.Val) extends RuntimeException(error.errMsg)

}
