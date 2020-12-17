package asura.ui.karate;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.intuit.karate.LogAppender;
import com.intuit.karate.Logger;
import com.intuit.karate.StepActions;
import com.intuit.karate.core.ExecutionContext;
import com.intuit.karate.core.ExecutionHook;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioContext;
import com.intuit.karate.core.ScenarioExecutionUnit;
import com.intuit.karate.core.StepResult;

public class KarateScenarioExecutionUnit extends ScenarioExecutionUnit {

  public KarateScenarioExecutionUnit(Scenario scenario,
    List<StepResult> results, ExecutionContext exec) {
    super(scenario, results, exec);
  }

  public KarateScenarioExecutionUnit(Scenario scenario,
    List<StepResult> results, ExecutionContext exec,
    ScenarioContext backgroundContext) {
    super(scenario, results, exec, backgroundContext);
  }

  @Override
  public void init() {
    ExecutionContext exec;
    LogAppender appender;
    Collection<ExecutionHook> hooks;
    try {
      Class<ScenarioExecutionUnit> clazz = ScenarioExecutionUnit.class;
      Field execField = clazz.getDeclaredField("exec");
      execField.setAccessible(true);
      exec = (ExecutionContext) execField.get(this);
      Field appenderField = clazz.getDeclaredField("appender");
      appenderField.setAccessible(true);
      appender = (LogAppender) appenderField.get(this);
      Field hooksField = clazz.getDeclaredField("hooks");
      hooksField.setAccessible(true);
      Field stepsField = clazz.getDeclaredField("steps");
      stepsField.setAccessible(true);
      boolean initFailed = false;
      StepActions actions = getActions();
      if (actions == null) {
        // karate-config.js will be processed here
        // when the script-context constructor is called
        try {
          actions = new KarateStepActions(exec.featureContext, exec.callContext, exec.classLoader, scenario, appender);
          setActions(actions);
        } catch (Exception e) {
          initFailed = true;
          result.addError("scenario init failed", e);
        }
      } else { // dynamic scenario outline, hack to swap logger for current thread
        Logger logger = new Logger();
        logger.setAppender(appender);
        actions.context.setLogger(logger);
      }
      if (!initFailed) { // actions will be null otherwise
        // this flag is used to suppress logs in the http client if needed
        actions.context.setReportDisabled(true);
        // this is not done in the constructor as we need to be on the "executor" thread
        hooks = exec.callContext.resolveHooks();
        hooksField.set(this, hooks);
        // before-scenario hook, important: actions.context will be null if initFailed
        if (hooks != null) {
          try {
            hooks.forEach(h -> h.beforeScenario(scenario, getActions().context));
          } catch (Exception e) {
            initFailed = true;
            result.addError("beforeScenario hook failed", e);
          }
        }
      }
      if (initFailed) {
        stepsField.set(this, Collections.EMPTY_LIST);
      } else {
        if (scenario.isDynamic()) {
          stepsField.set(this, scenario.getBackgroundSteps());
        } else {
          if (scenario.isBackgroundDone()) {
            stepsField.set(this, scenario.getSteps());
          } else {
            stepsField.set(this, scenario.getStepsIncludingBackground());
          }
          if (scenario.isOutline()) { // init examples row magic variables
            Map<String, Object> exampleData = scenario.getExampleData();
            actions.context.vars.put("__row", exampleData);
            actions.context.vars.put("__num", scenario.getExampleIndex());
            if (actions.context.getConfig().isOutlineVariablesAuto()) {
              exampleData.forEach((k, v) -> getActions().context.vars.put(k, v));
            }
          }
        }
      }
      result.setThreadName(Thread.currentThread().getName());
      result.setStartTime(System.currentTimeMillis() - exec.startTime);
    } catch (Throwable t) {
      throw new RuntimeException(t.getMessage());
    }
  }
}
