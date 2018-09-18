package asura.core.auth

import java.util.concurrent.ConcurrentHashMap

object AuthManager {

  private val operators = new ConcurrentHashMap[String, AuthorizeAndValidate]()

  def register(operator: AuthorizeAndValidate): Unit = {
    if (operators.contains(operator.`type`)) {
      throw new RuntimeException(s"${operator.`type`} already exists.")
    } else {
      operators.put(operator.`type`, operator)
    }
  }

  def apply(name: String): Option[AuthorizeAndValidate] = {
    Option(operators.get(name))
  }

  def getAll(): java.util.Collection[AuthorizeAndValidate] = {
    operators.values()
  }
}
