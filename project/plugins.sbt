resolvers += Resolver.url("sbt-plugin-snapshots", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("org.jetbrains" % "sbt-teamcity-logger" % "0.1.0-SNAPSHOT")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.1.10")
