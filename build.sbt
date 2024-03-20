import dependencies._
import utils.commonScalacOptions
import scala.language.postfixOps

name := """edge"""
organization := """io.github.ergo-lend"""
organizationName := "exle"
organizationHomepage := Some(url("https://github.com/ergo-lend/edge"))
scalaVersion := "2.13.9"
version := "0.1.14-SNAPSHOT"
description := "Ergo Development Generics"
licenses := Seq(
  "Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
homepage := Some(url("https://github.com/Ergo-Lend/edge"))

lazy val NexusReleases = "Sonatype Releases".at(
  "https://s01.oss.sonatype.org/content/repositories/releases"
)

lazy val NexusSnapshots = "Sonatype Snapshots".at(
  "https://s01.oss.sonatype.org/content/repositories/snapshots"
)

lazy val commonSettings = List(
  scalacOptions ++= commonScalacOptions,
  resolvers ++= Seq(
    NexusReleases,
    NexusSnapshots
  ) ++ Resolver.sonatypeOssRepos("public")
    ++ Resolver.sonatypeOssRepos("snapshots"),
  pomExtra :=
    <developers>
      <developer>
        <id>kii</id>
        <name>Kii</name>
      </developer>
      <developer>
        <id>k-singh</id>
        <name>Cheese Enthusiast</name>
      </developer>
    </developers>,
  libraryDependencies ++= Testing
)

versionScheme := Some("early-semver")
publishMavenStyle := true
publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots".at(nexus + "content/repositories/snapshots"))
  else Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
}
ThisBuild / pomIncludeRepository := { _ => false }
scmInfo := Some(
  ScmInfo(
    url("https://github.com/Ergo-Lend/edge"),
    "scm:git@github.com:Ergo-Lend/edge.git"
  )
)

// prefix version with "-SNAPSHOT" for builds without a git tag
ThisBuild / dynverSonatypeSnapshots := false
// use "-" instead of default "+"
ThisBuild / dynverSeparator := "-"
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      Ergo ++
        Testing ++
        DependencyInjection ++
        HttpDep
  )

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

Compile / packageDoc / publishArtifact := false
