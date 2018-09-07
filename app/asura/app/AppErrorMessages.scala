package asura.app

import asura.core.ErrorMessages.Val

object AppErrorMessages {
  val error_EmptyProfile = Val("Profile is not present")
  val error_TokenGeneratedError = Val("fail to generate JWT token")
  val error_FailToCreateUser = Val("fail to create user")
}
