package com.example.asura.assertion

import asura.core.cs.assertion.Assertion
import asura.core.cs.assertion.engine.{AssertResult, PassAssertResult}

import scala.concurrent.Future

object ExampleAssertion extends Assertion {

  override val name: String = "$example"
  override val description: String = ""

  /**
    *
    * @param actual the actual value from context
    * @param expect the expect value
    * @return
    */
  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(PassAssertResult())
  }
}
