import com.twitter.scrooge.ScroogeSBT

import sbt.Keys._
import sbt._

object build extends Build {
  val sharedSettings = Project.defaultSettings ++ Seq(
    organization := "com.github.dcapwell",
    scalaVersion := "2.10.4",
    version := "0.1.0",
    crossScalaVersions := Seq("2.10.4"),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    javacOptions in doc := Seq("-source", "1.7"),
    parallelExecution in Test := true,
    scalacOptions ++= Seq(
      Opts.compile.unchecked, 
      Opts.compile.deprecation, 
      Opts.compile.explaintypes,
      "-feature"
    )
  ) ++ ScroogeSBT.newSettings

  val scalazVersion = "7.0.6"
  val thriftVersion = "0.9.1"
  // val thriftVersion = "0.8.0"
  val scroogeVersion  = "3.16.1"
  val scalatestVersion = "2.2.0"
  val nettyVersion = "4.0.21.Final"
  val guavaVersion = "18.0"
  val logbackVersion = "1.1.2"

  lazy val root = Project(id = "netty-proto", base = file("."), settings = sharedSettings ++ Seq(
    Keys.name := "netty-proto"
  )).settings(
    libraryDependencies ++= Seq(
      "org.scalaz"          %% "scalaz-core" % scalazVersion,
      "org.apache.thrift"   % "libthrift" % thriftVersion,
      "com.twitter"         %% "scrooge-core" % scroogeVersion,

      "io.netty"            % "netty-all" % nettyVersion,
      "com.google.guava"    % "guava" % guavaVersion,
      "ch.qos.logback"      % "logback-classic" % logbackVersion,

      "org.scalatest"       %% "scalatest" % scalatestVersion % "test"
    )
  )
}


// vim: set ts=4 sw=4 et:
