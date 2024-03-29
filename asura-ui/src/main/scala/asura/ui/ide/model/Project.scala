package asura.ui.ide.model

import asura.common.util.StringUtils

case class Project(
                    var workspace: String,
                    var name: String,
                    var alias: String = StringUtils.EMPTY,
                    var avatar: String = StringUtils.EMPTY,
                    var description: String = StringUtils.EMPTY,
                  ) extends AbsDoc
