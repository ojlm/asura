package asura.app

import asura.common.exceptions.ErrorMessages.ErrorMessage

object AppErrorMessages {
  val error_EmptyProfile = "error_EmptyProfile"
  val error_TokenGeneratedError = "error_TokenGeneratedError"
  val error_FailToCreateUser = "error_FailToCreateUser"
  val error_InvalidCronExpression = "error_InvalidCronExpression"
  val error_NotAllowedContactAdministrator = "error_NotAllowedContactAdministrator"
  val error_CantDeleteCase = "error_CantDeleteCase"
  val error_CantDeleteScenario = "error_CantDeleteScenario"
  val error_CantDeleteEnv = "error_CantDeleteEnv"
  val error_ClusterNotEnabled = "error_ClusterNotEnabled"
  val error_CanNotUseReservedGroup = "error_CanNotUseReservedGroup"
  val error_UserAlreadyJoined = "error_UserAlreadyJoined"

  val error_FileNotExist = ErrorMessage("File not exists")("error_FileNotExist")
  val error_EmptyBlobStoreDir = ErrorMessage("Empty blob store directory")("error_EmptyBlobStoreDir")
  val error_AccessDenied = ErrorMessage("Access Denied")("error_AccessDenied")
  val error_NonActiveStoreEngine = ErrorMessage("Non active store engine")("error_NonActiveStoreEngine")
}
