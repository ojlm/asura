package asura.ui.karate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.intuit.karate.Logger;
import com.intuit.karate.Resource;
import com.intuit.karate.Script;
import com.intuit.karate.ScriptValue;
import com.intuit.karate.core.ExecutionContext;
import com.intuit.karate.core.Feature;
import com.intuit.karate.core.FeatureExecutionUnit;
import com.intuit.karate.core.FeatureSection;
import com.intuit.karate.core.Scenario;
import com.intuit.karate.core.ScenarioContext;
import com.intuit.karate.core.ScenarioExecutionUnit;

public class KarateFeature extends Feature {

  public KarateExtension extension;

  public KarateFeature(Resource resource, KarateExtension extension) {
    super(resource);
    this.extension = extension;
  }

  // return custom ScenarioExecutionUnit
  @Override
  public List<ScenarioExecutionUnit> getScenarioExecutionUnits(ExecutionContext exec) {
    List<ScenarioExecutionUnit> units = new ArrayList();
    for (FeatureSection section : getSections()) {
      if (section.isOutline()) {
        for (Scenario scenario : section.getScenarioOutline().getScenarios()) {
          if (scenario.isDynamic()) {
            if (!FeatureExecutionUnit.isSelected(exec.featureContext, scenario, new Logger())) { // throwaway logger
              continue;
            }
            ScenarioExecutionUnit bgUnit = new KarateScenarioExecutionUnit(scenario, null, exec, extension);
            bgUnit.run();
            ScenarioContext bgContext = bgUnit.getContext();
            if (bgContext == null || bgUnit.isStopped()) { // karate-config.js || background failed
              units.add(bgUnit); // exit early
              continue;
            }
            String expression = scenario.getDynamicExpression();
            ScriptValue listValue;
            try {
              listValue = Script.evalKarateExpression(expression, bgContext);
            } catch (Exception e) {
              String message = "dynamic expression evaluation failed: " + expression;
              bgUnit.result.addError(message, e);
              units.add(bgUnit); // exit early
              continue;
            }
            if (listValue.isListLike()) {
              List list = listValue.getAsList();
              int count = list.size();
              for (int i = 0; i < count; i++) {
                ScriptValue rowValue = new ScriptValue(list.get(i));
                if (rowValue.isMapLike()) {
                  Scenario dynamic = scenario.copy(i); // this will set exampleIndex
                  dynamic.setBackgroundDone(true);
                  Map<String, Object> map = rowValue.getAsMap();
                  dynamic.setExampleData(map); // and here we set exampleData
                  map.forEach((k, v) -> {
                    ScriptValue sv = new ScriptValue(v);
                    dynamic.replace("<" + k + ">", sv.getAsString());
                  });
                  ScenarioExecutionUnit unit =
                    new KarateScenarioExecutionUnit(dynamic, bgUnit.result.getStepResults(), exec, bgContext,
                      extension);
                  units.add(unit);
                } else {
                  bgContext.logger.warn("ignoring dynamic expression list item {}, not map-like: {}", i, rowValue);
                }
              }
            } else {
              bgContext.logger
                .warn("ignoring dynamic expression, did not evaluate to list: {} - {}", expression, listValue);
            }
          } else {
            units.add(new KarateScenarioExecutionUnit(scenario, null, exec, extension));
          }
        }
      } else {
        units.add(new KarateScenarioExecutionUnit(section.getScenario(), null, exec, extension));
      }
    }
    return units;
  }

}
