package asura.app.hook

import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class ApplicationStart @Inject()(lifecycle: ApplicationLifecycle) {

  lifecycle.addStopHook { () =>
    Future.successful(())
  }
}
