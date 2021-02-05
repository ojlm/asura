package asura.ui.karate

import java.util
import java.util.Collections

import com.intuit.karate.core._
import com.intuit.karate.driver.Driver
import com.intuit.karate.exception.KarateAbortException
import com.intuit.karate.{CallContext, Resource}

import scala.collection.mutable.ArrayBuffer

object KarateRunner {

  // empty feature for override because of the karate api limitation
  val ORIGIN: Feature = new Feature(Resource.EMPTY)
  val PATTERNS: util.Collection[MethodPattern] = {
    val field = classOf[Engine].getDeclaredField("PATTERNS")
    field.setAccessible(true)
    field.get(null).asInstanceOf[util.Collection[MethodPattern]]
  }

  @inline
  def buildCallContext(
                        vars: util.Map[String, AnyRef],
                        hook: ExecutionHook = null,
                        evalKarateConfig: Boolean = false
                      ): CallContext = {
    if (hook == null) {
      new CallContext(vars, evalKarateConfig)
    } else {
      new CallContext(vars, evalKarateConfig, hook)
    }
  }

  def buildStepActions(
                        driver: Driver,
                        vars: util.Map[String, AnyRef] = Collections.emptyMap(),
                        hook: ExecutionHook = null,
                        evalKarateConfig: Boolean = false
                      ): KarateStepActions = {
    val callContext = buildCallContext(vars, hook, evalKarateConfig)
    val featureContext = new FeatureContext(null, KarateRunner.ORIGIN, null)
    new KarateStepActions(featureContext, callContext, new KarateExtension(driver))
  }

  def runFeature(
                  text: String,
                  vars: util.Map[String, AnyRef],
                  hook: ExecutionHook = null,
                  evalKarateConfig: Boolean = false,
                ): FeatureResult = {
    val feature: Feature = parseFeature(text)
    val callContext = buildCallContext(vars, hook, evalKarateConfig)
    val featureContext = new FeatureContext(null, feature, null)
    val exec = new ExecutionContext(
      null, System.currentTimeMillis(), featureContext, callContext,
      null, null, null
    )
    val unit = new FeatureExecutionUnit(exec)
    unit.run()
    exec.result
  }

  def executeStep(
                   step: String,
                   driver: Driver,
                   vars: util.Map[String, AnyRef] = Collections.emptyMap(),
                   hook: ExecutionHook = null,
                   evalKarateConfig: Boolean = false,
                 ): KarateResult = {
    val actions = buildStepActions(driver, vars, hook, evalKarateConfig)
    executeStep(step)(actions)
  }

  def executeSteps(steps: Seq[String])(implicit actions: KarateStepActions): Seq[KarateResult] = {
    steps.map(s => executeStep(s))
  }

  def executeStep(step: String)(implicit actions: KarateStepActions): KarateResult = {
    val matches = findMethodsMatching(step)
    if (matches.isEmpty) {
      val error = s"no step-definition method match found for: $step"
      KarateResult.failed(error)
    } else if (matches.size > 1) {
      val error = s"more than one step-definition method matched: $step"
      KarateResult.failed(error)
    } else {
      val matchItem = matches(0)
      val startTime = System.nanoTime()
      try {
        val args = matchItem.convertArgs(null)
        matchItem.method.invoke(actions, args: _*)
        KarateResult.passed(System.nanoTime() - startTime)
      } catch {
        case _: KarateAbortException =>
          KarateResult.aborted(System.nanoTime() - startTime)
        case t: Throwable =>
          KarateResult.failed(t.getMessage)
      }
    }
  }

  def parseFeature(text: String, extension: KarateExtension = null): KarateFeature = {
    val feature = ORIGIN.replaceText(text)
    val karateFeature = new KarateFeature(ORIGIN.getResource(), extension)
    karateFeature.setLine(feature.getLine())
    karateFeature.setTags(feature.getTags())
    karateFeature.setName(feature.getName())
    karateFeature.setDescription(feature.getDescription())
    karateFeature.setBackground(feature.getBackground())
    karateFeature.setSections(feature.getSections())
    karateFeature.setLines(feature.getLines())
    karateFeature.setCallTag(feature.getCallTag())
    karateFeature.setCallName(feature.getCallName())
    karateFeature.setCallLine(feature.getCallLine())
    karateFeature
  }

  def parseFeatureSummary(text: String): KarateFeatureSummary = {
    val feature = ORIGIN.replaceText(text)
    KarateFeatureSummary(
      name = feature.getName,
      description = feature.getDescription,
      lineCount = if (feature.getLines != null) feature.getLines.size() else 0,
      sectionCount = if (feature.getSections != null) feature.getSections.size() else 0,
    )
  }

  def findMethodsMatching(text: String): Seq[MethodMatch] = {
    val matches = ArrayBuffer[MethodMatch]()
    PATTERNS.forEach(pattern => {
      val args = pattern.`match`(text)
      if (args != null) {
        matches += new MethodMatch(pattern.method, args)
      }
    })
    matches.toSeq
  }

}
