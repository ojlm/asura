package asura.ui.karate

import java.util

import com.intuit.karate.core._
import com.intuit.karate.{CallContext, Resource}

object KarateRunner {

  // empty feature for override because of the karate api limitation
  private val ORIGIN: Feature = new Feature(Resource.EMPTY)

  def runFeature(
                  text: String,
                  vars: util.Map[String, AnyRef],
                  hook: ExecutionHook = null,
                  evalKarateConfig: Boolean = false,
                ): FeatureResult = {
    val feature: Feature = parseFeature(text)
    val callContext = if (hook == null) {
      new CallContext(vars, evalKarateConfig)
    } else {
      new CallContext(vars, evalKarateConfig, hook)
    }
    val featureContext = new FeatureContext(null, feature, null)
    val exec = new ExecutionContext(
      null, System.currentTimeMillis(), featureContext, callContext,
      null, null, null
    )
    val unit = new FeatureExecutionUnit(exec)
    unit.run()
    exec.result
  }

  def parseFeature(text: String): KarateFeature = {
    val feature = ORIGIN.replaceText(text)
    val karateFeature = new KarateFeature(ORIGIN.getResource())
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
}
