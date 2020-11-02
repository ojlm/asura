package asura.core.security

case class AuthResponse(
                         allowed: Boolean,
                         maintainers: Maintainers = null,
                       )
