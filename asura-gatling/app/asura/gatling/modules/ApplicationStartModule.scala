package asura.gatling.modules

import asura.gatling.hook.ApplicationStart
import com.google.inject.AbstractModule

class ApplicationStartModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }
}
