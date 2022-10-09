import dependencies._
import utils.commonScalacOptions
import scala.language.postfixOps

name := """edge"""
organization := """io.exle"""
scalaVersion := "2.12.15"
version := "1.0"

lazy val NexusReleases = "Sonatype Releases".at(
  "https://s01.oss.sonatype.org/content/repositories/releases"
)

lazy val NexusSnapshots = "Sonatype Snapshots".at(
  "https://s01.oss.sonatype.org/content/repositories/snapshots"
)


lazy val commonSettings = List(
  scalacOptions ++= commonScalacOptions,
  scalaVersion := "2.12.15",
  organization := "io.exle",
  version := "0.1",
  resolvers ++= Seq(
    Resolver.sonatypeRepo("public"),
    Resolver.sonatypeRepo("snapshots"),
    NexusReleases,
    NexusSnapshots
  ),
  licenses := Seq("CC0" -> url("https://creativecommons.org/publicdomain/zero/1.0/legalcode")),
  homepage := Some(url("https://github.com/Ergo-Lend/edge")),
  description := "Ergo Development Generics",
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
  publishMavenStyle := true,
  publishTo := sonatypePublishToBundle.value,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/Ergo-Lend/edge"),
      "scm:git@github.com:Ergo-Lend/edge.git"
    )
  ),
  libraryDependencies ++= Testing ++
    Enumeratum
)

// prefix version with "-SNAPSHOT" for builds without a git tag
dynverSonatypeSnapshots in ThisBuild := true
// use "-" instead of default "+"
dynverSeparator in ThisBuild := "-"

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
