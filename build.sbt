organization := "tech.minna"
name := "play-json-macros"

scalaVersion := "2.13.0"
crossScalaVersions := Seq("2.12.9", "2.13.0")

scalacOptions in ThisBuild ++= {
  val annotationCompilerOptionMaybe = CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 13 => Some("-Ymacro-annotations")
    case _ => None
  }

  Seq("-unchecked", "-deprecation") ++ annotationCompilerOptionMaybe.toSeq
}

libraryDependencies ++= {
  val paradiseCompilerPluginMaybe = CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 13 => None
    case _ => Some(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  }

  Seq(
    "com.typesafe.play" %% "play-json" % "2.7.4" % Provided,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "3.0.8" % Test
  ) ++ paradiseCompilerPluginMaybe.toSeq
}

// Publishing

bintrayOrganization := Some("minna-technologies")
bintrayReleaseOnPublish in ThisBuild := true

releaseCrossBuild := true
useGpg := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))