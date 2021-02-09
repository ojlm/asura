package asura.ui.model

import com.fasterxml.jackson.annotation.JsonAlias

case class ChromeVersion(
                          @JsonAlias(Array("Browser"))
                          browser: String,
                          @JsonAlias(Array("Protocol-Version"))
                          protocolVersion: String,
                          @JsonAlias(Array("User-Agent"))
                          userAgent: String,
                          @JsonAlias(Array("V8-Version"))
                          v8Version: String,
                          @JsonAlias(Array("WebKit-Version"))
                          webKitVersion: String,
                        )
