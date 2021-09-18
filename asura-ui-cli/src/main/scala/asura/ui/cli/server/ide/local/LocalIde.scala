package asura.ui.cli.server.ide.local

import asura.common.util.StringUtils
import asura.ui.cli.CliSystem
import asura.ui.cli.server.ide.local.ops.{LocalActivityOps, LocalProjectOps, LocalUserOps, LocalWorkspaceOps}
import asura.ui.ide.Ide

class LocalIde(val config: LocalConfig) extends Ide {

  private implicit val ec = CliSystem.ec

  override lazy val activity: LocalActivityOps = new LocalActivityOps(this)
  override lazy val user: LocalUserOps = new LocalUserOps(this)
  override lazy val workspace: LocalWorkspaceOps = new LocalWorkspaceOps(this)
  override lazy val project: LocalProjectOps = new LocalProjectOps(this)

}

object LocalIde {

  val DEFAULT_USERNAME = StringUtils.notEmptyElse(System.getProperty("user.name"), "indigo")

}
