package asura.core.security

case class UserRoles(
                      groups: java.util.Map[String, MemberRoleItem],
                      projects: java.util.Map[String, MemberRoleItem],
                    )
