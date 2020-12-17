package asura.ui.karate;

import java.util.function.Function;

import com.intuit.karate.CallContext;
import com.intuit.karate.LogAppender;
import com.intuit.karate.ScriptBindings;
import com.intuit.karate.core.FeatureContext;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioContext;

public class KarateScenarioContext extends ScenarioContext {

  public KarateScenarioContext(
    FeatureContext featureContext,
    CallContext call,
    Scenario scenario,
    LogAppender appender
  ) {
    super(featureContext, call, scenario, appender);
    doMore(this);
  }

  public KarateScenarioContext(
    FeatureContext featureContext,
    CallContext call,
    ClassLoader classLoader,
    Scenario scenario,
    LogAppender appender
  ) {
    super(featureContext, call, classLoader, scenario, appender);
    doMore(this);
  }

  @Override
  public ScenarioContext copy() {
    ScenarioContext copy = super.copy();
    doMore(copy);
    return copy;
  }

  public static void doMore(ScenarioContext ctx) {
    ctx.bindings.adds.put(ScriptBindings.READ, readX(ctx));
  }

  // TODO: support read from other storage
  public static Function<String, Object> readX(ScenarioContext ctx) {
    return s -> {
      return ctx.read.apply(s);
    };
  }

}
