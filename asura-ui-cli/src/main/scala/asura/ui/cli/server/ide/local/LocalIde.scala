package asura.ui.cli.server.ide.local

import asura.common.util.StringUtils
import asura.ui.cli.CliSystem
import asura.ui.cli.server.ide.local.ops._
import asura.ui.ide.Ide

class LocalIde(val config: LocalConfig) extends Ide {

  private implicit val ec = CliSystem.ec

  override lazy val activity: LocalActivityOps = new LocalActivityOps(this)
  override lazy val user: LocalUserOps = new LocalUserOps(this)
  override lazy val workspace: LocalWorkspaceOps = new LocalWorkspaceOps(this)
  override lazy val project: LocalProjectOps = new LocalProjectOps(this)
  override lazy val tree: LocalTreeStoreOps = new LocalTreeStoreOps(this)
  override lazy val blob: LocalBlobStoreOps = new LocalBlobStoreOps(this)

}

object LocalIde {

  val DEFAULT_USERNAME = StringUtils.notEmptyElse(System.getProperty("user.name"), "indigo")

  def isNameLegal(name: String): Boolean = {
    if (StringUtils.isEmpty(name) || RESERVED_NAMES.contains(name)) {
      false
    } else {
      name.forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c.equals('_') || c.equals('-') || c.equals('.'))
    }
  }

  lazy val RESERVED_NAMES = Set(
    "home", "dashboard", "discover", "team", "workspace", "projects", "project", "stars", "tasks",
    "job", "running", "chrome", "android", "ios", "search"
  )

}
