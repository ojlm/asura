package com.example.asura

import com.google.inject.AbstractModule

class ExampleModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ExampleBootstrap]).asEagerSingleton()
  }
}
