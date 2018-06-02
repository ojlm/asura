package asura.common.model

case class Credential(username: String, password: String, var token: String = null, var isPassRight: Boolean = true)
