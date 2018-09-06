package asura.core.notify

case class NotifyResponses(
                            success: Seq[NotifyResponse],
                            failure: Seq[NotifyResponse]
                          ) {

  def isSuccessful(): Boolean = {
    if (null == failure) {
      true
    } else {
      failure.nonEmpty
    }
  }
}

object NotifyResponses {
  def apply(responses: Seq[NotifyResponse]): NotifyResponses = {
    val map = responses.filter(null != _).groupBy(_.isOk)
    NotifyResponses(map.getOrElse(true, Nil), map.getOrElse(false, Nil))
  }
}
