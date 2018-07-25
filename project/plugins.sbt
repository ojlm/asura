resolvers += Resolver.mavenLocal
resolvers += "Aliyun" at "http://maven.aliyun.com/nexus/content/groups/public/"
resolvers += Resolver.DefaultMavenRepository
externalResolvers := resolvers.value

// Pure Scala Artifact Fetching https://github.com/coursier/coursier
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0")
// addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.3")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.16")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.1")
