import sbt._
import versions._
import play.sbt.PlayImport.guice

object dependencies {

  val Ergo: List[ModuleID] = List(
    "org.scorexfoundation" %% "scrypto"     % ScryptoVersion,
    "org.ergoplatform"     %% "ergo-appkit" % ErgoAppKitVersion,
    "org.scorexfoundation" %% "sigma-state" % SigmaStateVersion
  )

  val Circe: List[ModuleID] = List(
    "com.dripower" %% "play-circe" % PlayCirceVersion
  )

  val Cats: List[ModuleID] = List(
    "org.typelevel" %% "cats-core" % CatsVersion
  )

  val PlayApi: List[ModuleID] = List(
    "org.playframework.anorm" %% "anorm"      % AnormVersion,
    "com.typesafe.play"       %% "play-slick" % PlaySlickVersion
  )

  val HttpDep: List[ModuleID] = List(
    "org.scalaj" %% "scalaj-http" % ScalaJHttpVersion
  )

  val Testing: List[ModuleID] = List(
    "org.scalatestplus.play" %% "scalatestplus-play" % ScalaTestPlusPlayVersion % Test
  )

  // Commons
  val Enumeratum: List[ModuleID] = List(
    "com.beachape" %% "enumeratum" % EnumeratumVersion
  )

  // Java
  val DependencyInjection: List[ModuleID] = List(
    guice
  )
}
