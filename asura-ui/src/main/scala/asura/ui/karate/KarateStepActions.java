package asura.ui.karate;

import java.lang.reflect.Field;

import com.intuit.karate.CallContext;
import com.intuit.karate.LogAppender;
import com.intuit.karate.StepActions;
import com.intuit.karate.core.FeatureContext;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioContext;

public class KarateStepActions extends StepActions {

  public KarateStepActions(
    FeatureContext featureContext, CallContext callContext,
    ClassLoader classLoader, Scenario scenario, LogAppender appender, KarateExtension extension
  ) throws NoSuchFieldException, IllegalAccessException {
    super(null);
    // use custom scenario context
    ScenarioContext context =
      new KarateScenarioContext(featureContext, callContext, classLoader, scenario, appender, extension);
    Field field = StepActions.class.getDeclaredField("context");
    field.setAccessible(true);
    field.set(this, context);
  }

  public KarateStepActions(
    FeatureContext featureContext,
    CallContext callContext,
    KarateExtension extension
  ) throws NoSuchFieldException, IllegalAccessException {
    super(null);
    // use custom scenario context
    ScenarioContext context = new KarateScenarioContext(featureContext, callContext, null, null, null, extension);
    Field field = StepActions.class.getDeclaredField("context");
    field.setAccessible(true);
    field.set(this, context);
  }
}
