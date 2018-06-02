package asura.web

import com.machinepublishers.jbrowserdriver.{Settings, Timezone, UserAgent}

object BrowserSettings {

  val chromeSettings = Settings.builder()
    .timezone(Timezone.ASIA_SHANGHAI)
    .cache(true)
    .ssl("trustanything")
    .userAgent(UserAgent.CHROME)
    .quickRender(true)
    .headless(true)
    .build()
}
