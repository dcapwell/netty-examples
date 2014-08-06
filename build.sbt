
val scalazVersion = "7.0.6"

organization := "com.github.dcapwell"

scalaVersion := "2.10.4"

version := "0.1.0"

crossScalaVersions := Seq("2.10.4", "2.11.1")

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

javacOptions in doc := Seq("-source", "1.7")

parallelExecution in Test := true

scalacOptions ++= Seq(
  Opts.compile.unchecked,
  Opts.compile.deprecation,
  Opts.compile.explaintypes,
  "-feature"
)

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % scalazVersion,
  "io.netty" % "netty-all" % "4.0.21.Final",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test"
)
