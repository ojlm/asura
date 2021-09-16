package asura.ui.ide

import asura.ui.ide.ops.{ActivityOps, ProjectOps, UserOps, WorkspaceOps}

trait Ide {

  val activity: ActivityOps
  val user: UserOps
  val workspace: WorkspaceOps
  val project: ProjectOps

}
