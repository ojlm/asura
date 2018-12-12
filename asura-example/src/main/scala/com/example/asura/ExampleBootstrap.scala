package com.example.asura

import akka.actor.ActorSystem
import asura.core.auth.AuthManager
import asura.core.cs.assertion.Assertions
import asura.core.notify.JobNotifyManager
import com.example.asura.assertion.ExampleAssertion
import com.example.asura.auth.ExampleAuth
import com.example.asura.notify.ExampleNotification
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

@Singleton
class ExampleBootstrap @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  system: ActorSystem,
                                  configuration: Configuration, // https://www.playframework.com/documentation/2.6.x/ConfigFile
                                ) {

  AuthManager.register(new ExampleAuth(configuration))

  JobNotifyManager.register(new ExampleNotification(configuration))

  Assertions.register(ExampleAssertion)
}
