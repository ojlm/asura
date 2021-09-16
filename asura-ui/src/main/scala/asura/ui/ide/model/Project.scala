package asura.ui.ide.model

case class Project(
                    var name: String,
                    var alias: String,
                    var workspace: String,
                    var avatar: String,
                    var description: String,
                  ) extends AbsDoc
