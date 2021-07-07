package com.intuit.karate;

import java.util.List;

public class BuilderEx extends Runner.Builder<BuilderEx> {

  @Override
  public Results parallel(int threadCount) {
    threads(threadCount);
    Suite suite = new Suite(this);
    suite.run();
    return Results.of(suite, true);
  }

  public static BuilderEx paths(String... paths) {
    BuilderEx builder = new BuilderEx();
    return builder.path(paths);
  }

  public static BuilderEx paths(List<String> paths) {
    BuilderEx builder = new BuilderEx();
    return builder.path(paths);
  }

}
