package asura.core.security

case class Maintainers(
                        group: String,
                        project: String,
                        groups: Seq[PermissionItem],
                        projects: Seq[PermissionItem],
                        admins: Seq[String] = Nil
                      )
