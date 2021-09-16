package asura.ui.cli.server.ide.remote

import asura.ui.ide.Ide
import asura.ui.ide.ops.{ActivityOps, ProjectOps, UserOps, WorkspaceOps}

class RemoteIde(val config: RemoteConfig) extends Ide {

  override val activity: ActivityOps = null
  override val user: UserOps = null
  override val workspace: WorkspaceOps = null
  override val project: ProjectOps = null

}
