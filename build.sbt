import dependencies._
import utils.commonScalacOptions

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
  libraryDependencies ++= Testing ++
    Enumeratum
)

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
