organization := "tech.minna"
name := "play-json-macros"

scalaVersion := "2.13.0"
crossScalaVersions := Seq("2.11.8", "2.12.4", "2.13.0")

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation", "-Ymacro-annotations")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.7.4" % Provided,
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)

// Publishing

bintrayOrganization := Some("minna-technologies")
bintrayReleaseOnPublish in ThisBuild := true

releaseCrossBuild := true
useGpg := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
