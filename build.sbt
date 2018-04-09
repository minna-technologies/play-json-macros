organization := "tech.minna"
name := "play-json-macros"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.8", "2.12.4")

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.6.9" % Provided,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

// Publishing

bintrayOrganization := Some("minna-technologies")
bintrayReleaseOnPublish in ThisBuild := true

releaseCrossBuild := true
useGpg := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
