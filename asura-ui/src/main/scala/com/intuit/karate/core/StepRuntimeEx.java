package com.intuit.karate.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.intuit.karate.Actions;
import com.intuit.karate.ScenarioActions;
import com.intuit.karate.core.StepRuntime.MethodMatch;
import com.intuit.karate.core.StepRuntime.MethodPattern;

import asura.ui.karate.KarateResult;
import cucumber.api.java.en.When;

public class StepRuntimeEx {

  private static final Collection<MethodPattern> PATTERNS;
  private static final Map<String, Collection<Method>> KEYWORDS_METHODS;
  public static final Collection<Method> METHOD_MATCH;

  static {
    Map<String, MethodPattern> temp = new HashMap();
    List<MethodPattern> overwrite = new ArrayList();
    KEYWORDS_METHODS = new HashMap();
    for (Method method : ScenarioActions.class.getMethods()) {
      When when = method.getDeclaredAnnotation(When.class);
      if (when != null) {
        String regex = when.value();
        MethodPattern methodPattern = new MethodPattern(method, regex);
        temp.put(regex, methodPattern);
        Collection<Method> keywordMethods =
          KEYWORDS_METHODS.computeIfAbsent(methodPattern.keyword, k -> new HashSet<>());
        keywordMethods.add(methodPattern.method);
      } else {
        Action action = method.getDeclaredAnnotation(Action.class);
        if (action != null) {
          String regex = action.value();
          MethodPattern methodPattern = new MethodPattern(method, regex);
          overwrite.add(methodPattern);
        }
      }
    }
    for (MethodPattern mp : overwrite) {
      temp.put(mp.regex, mp);
      Collection<Method> keywordMethods = KEYWORDS_METHODS.computeIfAbsent(mp.keyword, k -> new HashSet<>());
      keywordMethods.add(mp.method);
    }
    PATTERNS = temp.values();
    METHOD_MATCH = StepRuntime.findMethodsByKeyword("match");
  }

  public static KarateResult execute(String text, Actions actions) {
    List<MethodMatch> matches = findMethodsMatching(text);
    if (matches.isEmpty()) {
      return KarateResult.fail("no step-definition method match found for: " + text);
    } else if (matches.size() > 1) {
      return KarateResult.fail("more than one step-definition method matched: " + text + " - " + matches);
    }
    MethodMatch match = matches.get(0);
    Object[] args;
    try {
      args = match.convertArgs(null);
    } catch (Exception ignored) {
      return KarateResult.fail("no step-definition method match found for: " + text);
    }
    long startTime = System.nanoTime();
    try {
      Object result = match.method.invoke(actions, args);
      if (actions.isAborted()) {
        return KarateResult.abort(getElapsedTimeNanos(startTime));
      } else if (actions.isFailed()) {
        return KarateResult.fail(actions.getFailedReason().getMessage(), getElapsedTimeNanos(startTime));
      } else {
        return KarateResult.pass(getElapsedTimeNanos(startTime), result);
      }
    } catch (InvocationTargetException e) {
      return KarateResult.fail(e, getElapsedTimeNanos(startTime));
    } catch (Exception e) {
      return KarateResult.fail(e, getElapsedTimeNanos(startTime));
    }
  }

  private static List<MethodMatch> findMethodsMatching(String text) {
    List<MethodMatch> matches = new ArrayList(1);
    for (MethodPattern pattern : PATTERNS) {
      List<String> args = pattern.match(text);
      if (args != null) {
        matches.add(new MethodMatch(pattern.method, args));
      }
    }
    return matches;
  }

  private static long getElapsedTimeNanos(long startTime) {
    return System.nanoTime() - startTime;
  }

}
