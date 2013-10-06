name := "storm-topology-dsl"

version := "0.8.1"

organization := "com.mariussoutier.storm"

libraryDependencies += "storm" % "storm" % "0.8.1"

libraryDependencies += "org.specs2" %% "specs2" % "2.2.2" % "test"

resolvers += "Clojars" at "http://clojars.org/repo"
