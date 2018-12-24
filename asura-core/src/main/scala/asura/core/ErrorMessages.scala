package asura.core

import com.sksamuel.elastic4s.http.Response

import scala.concurrent.Future

object ErrorMessages {

  val error_IllegalGroupId = ErrorMessage("Illegal group id")("error_IllegalGroupId")
  val error_GroupExists = ErrorMessage("Group already exists")("error_GroupExists")
  val error_IdNonExists = ErrorMessage("Id non exists")("error_IdNonExists")
  val error_GroupIdEmpty = ErrorMessage("Group id must not be empty")("error_GroupIdEmpty")
  val error_IllegalProjectId = ErrorMessage("Illegal project id")("error_IllegalProjectId")
  val error_ProjectExists = ErrorMessage("Project already exists")("error_ProjectExists")
  val error_EmptyRequestBody = ErrorMessage("Empty request body")("error_EmptyRequestBody")
  val error_EmptyGroup = ErrorMessage("Empty group")("error_EmptyGroup")
  val error_EmptyProject = ErrorMessage("Empty project")("error_EmptyProject")
  val error_EmptyPath = ErrorMessage("Empty path")("error_EmptyPath")
  val error_EmptyMethod = ErrorMessage("Empty method")("error_EmptyMethod")
  val error_EmptyCase = ErrorMessage("Empty case")("error_EmptyCase")
  val error_EmptySummary = ErrorMessage("Empty summary")("error_EmptySummary")
  val error_EmptyRequest = ErrorMessage("Empty request body")("error_EmptyRequest")
  val error_EmptyNamespace = ErrorMessage("Empty namespace")("error_EmptyNamespace")
  val error_EmptyId = ErrorMessage("Empty id")("error_EmptyId")
  val error_IndexSuccess = ErrorMessage("Create success")("error_IndexSuccess")
  val error_UpdateSuccess = ErrorMessage("Update success")("error_UpdateSuccess")
  val error_ServerError = ErrorMessage("Server Error")("error_ServerError")
  val error_EmptyEnv = ErrorMessage("Empty env")("error_EmptyEnv")
  val error_EmptyScenario = ErrorMessage("Empty scenario")("error_EmptyScenario")
  val error_EmptyJobName = ErrorMessage("Empty job name")("error_EmptyJobName")
  val error_EmptyJobType = ErrorMessage("Empty job type")("error_EmptyJobType")
  val error_EmptyJobId = ErrorMessage("Empty job id")("error_EmptyJobId")
  val error_EmptyScheduler = ErrorMessage("Empty scheduler")("error_EmptyScheduler")
  val error_InvalidToken = ErrorMessage("Invalid token")("error_InvalidToken")
  val error_EmptyJobCaseScenarioCount = ErrorMessage("Empty case and scenario count in job")("error_EmptyJobCaseScenarioCount")
  val error_NullKeyOrValue = ErrorMessage("Null key or value")("error_NullKeyOrValue")
  val error_UnknownMessageType = ErrorMessage("Unknown message type")("error_UnknownMessageType")
  val error_EmptyUsername = ErrorMessage("Empty username")("error_EmptyUsername")
  val error_EmptySubscriber = ErrorMessage("Empty subscriber")("error_EmptySubscriber")
  val error_EmptyNotifyType = ErrorMessage("Empty notify type")("error_EmptyNotifyType")
  val error_ProxyDisabled = ErrorMessage("Proxy is disabled")("error_ProxyDisabled")
  val error_EmptyDate = ErrorMessage("Empty date field")("error_EmptyDate")
  val error_InvalidRequestParameters = ErrorMessage("Invalid request parameters")("error_InvalidRequestParameters")

  def error_EsRequestFail(response: Response[_]) = ErrorMessage(response.error.reason)("error_EsRequestFail")

  def error_NoSchedulerDefined(scheduler: String) = ErrorMessage(scheduler)("error_NoSchedulerDefined")

  def error_NoJobDefined(job: String) = ErrorMessage(job)("error_NoJobDefined")

  def error_JobValidate(msg: String) = ErrorMessage(msg)("error_JobValidate")

  def error_NoNotifyImplementation(`type`: String) = ErrorMessage(`type`)("error_NoNotifyImplementation")

  def error_ReportNotifyError(msg: String) = ErrorMessage(msg)("error_ReportNotifyError")

  def error_NotRegisteredAuth(authType: String) = ErrorMessage(authType)("error_NotRegisteredAuth")

  def error_DuplicateApi(msg: String) = ErrorMessage(s"Duplicate api: $msg")("error_DuplicateApi")

  def error_Throwable(t: Throwable) = ErrorMessage(t.getMessage, t)("error_Throwable")

  def error_IdsNotFound(ids: Seq[String]) = ErrorMessage(ids.mkString(","))("error_IdsNotFound")

  def error_Msgs(msgs: Seq[String]) = ErrorMessage(msgs.mkString(","))("error_Msgs")

  def error_IllegalCharacter(msg: String) = ErrorMessage(msg)("error_IllegalCharacter")

  case class ErrorMessage(val errMsg: String, val t: Throwable = null)(_name: String) {

    def toException: ErrorMessageException = {
      ErrorMessageException(this)
    }

    def toFutureFail: Future[Nothing] = {
      Future.failed(ErrorMessageException(this))
    }

    val name = _name
  }

  case class ErrorMessageException(error: ErrorMessages.ErrorMessage) extends RuntimeException(error.errMsg)

}
