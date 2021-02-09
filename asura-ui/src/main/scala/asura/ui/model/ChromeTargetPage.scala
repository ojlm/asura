package asura.ui.model

case class ChromeTargetPage(
                             id: String,
                             `type`: String,
                             title: String,
                             url: String,
                             parentId: String,
                             description: String,
                             devtoolsFrontendUrl: String,
                             webSocketDebuggerUrl: String,
                           )
