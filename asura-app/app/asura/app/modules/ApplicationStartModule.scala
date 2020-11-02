package asura.app.modules

import java.time.Duration

import asura.app.hook.ApplicationStart
import asura.app.security.DefaultPermissionAuthProvider
import asura.core.security.PermissionAuthProvider
import com.google.inject.{AbstractModule, Provides}

class ApplicationStartModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }

  @Provides
  def provideCasClient: PermissionAuthProvider = {
    DefaultPermissionAuthProvider(
      1000, Duration.ofMinutes(2),
      1000, Duration.ofMinutes(2),
      1000, Duration.ofMinutes(2),
    )
  }
}
