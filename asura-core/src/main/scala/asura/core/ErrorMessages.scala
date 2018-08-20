package asura.core

import com.sksamuel.elastic4s.http.RequestFailure

import scala.concurrent.Future

object ErrorMessages extends Enumeration {

  // message enumeration
  def error_EsRequestFail(failure: RequestFailure) = Val(failure.error.reason)

  val error_IllegalGroupId = Val("Illegal group id")
  val error_GroupExists = Val("Group already exists")
  val error_IdNonExists = Val("Id non exists")

  protected case class Val(val errMsg: String) extends super.Val {
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
