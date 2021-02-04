mainClass in assembly := Some("asura.ui.cli.Main")
assemblyJarName in assembly := "asura-cli.jar"
assemblyMergeStrategy in assembly := {
  case PathList("cucumber", "api", xs@_*) => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case x =>
    println(x)
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
