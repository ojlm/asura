package asura.app.modules

import asura.app.hook.ApplicationStart
import asura.app.providers.{DefaultPermissionAuthProvider, DefaultUiDriverProvider}
import asura.core.security.PermissionAuthProvider
import asura.ui.driver.UiDriverProvider
import com.google.inject.{AbstractModule, Provides}
import play.api.{Configuration, Environment}

class ApplicationStartModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }

  @Provides
  def authProvider: PermissionAuthProvider = {
    DefaultPermissionAuthProvider(configuration)
  }

  @Provides
  def uiDriverProvider: UiDriverProvider = {
    DefaultUiDriverProvider(configuration)
  }

}
