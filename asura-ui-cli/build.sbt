mainClass in assembly := Some("asura.ui.cli.Main")
assemblyJarName in assembly := "asura-cli.jar"
assemblyMergeStrategy in assembly := {
  case PathList("com", "intuit", xs@_*) => MergeStrategy.first
  case PathList("cucumber", "api", xs@_*) => MergeStrategy.first
  case x@PathList(ps@_*) =>
    ps.last match {
      case "module-info.class" => MergeStrategy.discard
      case "jni-config.json" | "reflect-config.json" => MergeStrategy.first
      case _ =>
        if (ps.last.startsWith("LICENSE")) {
          MergeStrategy.discard
        } else {
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
        }
    }
}
