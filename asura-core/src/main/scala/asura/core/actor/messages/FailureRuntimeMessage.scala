package asura.core.actor.messages

object FailureRuntimeMessage {

  def apply(msg: String) = akka.actor.Status.Failure(new RuntimeException(msg))
  def apply(t: Throwable) = akka.actor.Status.Failure(t)
}
