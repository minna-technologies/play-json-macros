organization := "tech.minna"
name := "play-json-macros"
version := "0.1.0"

scalaVersion := "2.12.3"
crossScalaVersions := Seq("2.11.8", "2.12.3")

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.6.2" % Provided,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "3.0.3" % "test"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
