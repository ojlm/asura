package asura.core

import com.sksamuel.elastic4s.http.RequestFailure

import scala.concurrent.Future

object ErrorMessages extends Enumeration {

  // message enumeration
  def error_EsRequestFail(failure: RequestFailure) = Val(failure.error.reason)

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

  def error_DuplicateApi(msg: String) = Val(s"Duplicate api: $msg")

  case class Val(val errMsg: String) extends super.Val {
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
