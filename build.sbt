organization := "tech.minna"
name := "play-json-macros"

scalaVersion := "2.13.11"
crossScalaVersions := Seq("2.12.18", "2.13.11")

ThisBuild/ scalacOptions ++= {
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
    "com.typesafe.play" %% "play-json" % "2.9.4" % Provided,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "3.2.15" % Test
  ) ++ paradiseCompilerPluginMaybe.toSeq
}

// Publishing
ThisBuild / publishMavenStyle := true

releaseCrossBuild := true
useGpg := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

ThisBuild / githubOwner := "minna-technologies"
ThisBuild / githubRepository := "play-json-macros"
ThisBuild / publishMavenStyle := true
