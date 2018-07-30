package asura.app.modules

import com.google.inject.AbstractModule
import org.pac4j.play.scala.{DefaultSecurityComponents, SecurityComponents}
import org.pac4j.play.{CallbackController, LogoutController}
import org.pac4j.play.store.{PlayCacheSessionStore, PlaySessionStore}
import play.api.{Configuration, Environment}

class SecurityModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[PlaySessionStore]).to(classOf[PlayCacheSessionStore])

    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/?login")
    callbackController.setMultiProfile(true)
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)

    // security components used in controllers
    bind(classOf[SecurityComponents]).to(classOf[DefaultSecurityComponents])


  }
}
