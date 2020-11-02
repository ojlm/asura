package asura.app.modules

import asura.app.hook.ApplicationStart
import asura.app.security.DefaultPermissionAuthProvider
import asura.core.security.PermissionAuthProvider
import com.google.inject.{AbstractModule, Provides}
import play.api.{Configuration, Environment}

class ApplicationStartModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }

  @Provides
  def provideCasClient: PermissionAuthProvider = {
    DefaultPermissionAuthProvider(configuration)
  }
}
