resolvers ++= Seq(
  Resolver.githubPackages("minna-technologies")
)

addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
