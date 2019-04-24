package asura.core

case class SecurityConfig(
                           val pubKeyBytes: Array[Byte] = Array.emptyByteArray,
                           val priKeyBytes: Array[Byte] = Array.emptyByteArray,
                           val maskText: String = "***"
                         )
