package asura.ui.ide

import asura.ui.ide.ops._

trait Ide {

  val activity: ActivityOps
  val user: UserOps
  val workspace: WorkspaceOps
  val project: ProjectOps
  val tree: TreeStoreOps
  val blob: BlobStoreOps

}
