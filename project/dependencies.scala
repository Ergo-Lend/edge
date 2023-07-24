import sbt._
import versions._
import play.sbt.PlayImport.guice

object dependencies {

  val Ergo: List[ModuleID] = List(
    "org.scorexfoundation" %% "scrypto"     % ScryptoVersion,
    "org.ergoplatform"     %% "ergo-appkit" % ErgoAppKitVersion,
    "org.scorexfoundation" %% "sigma-state" % SigmaStateVersion
  )

  val HttpDep: List[ModuleID] = List(
    "org.scalaj" %% "scalaj-http" % ScalaJHttpVersion
  )

  val Testing: List[ModuleID] = List(
    "org.scalatestplus.play" %% "scalatestplus-play" % ScalaTestPlusPlayVersion % Test
  )

  // Java
  val DependencyInjection: List[ModuleID] = List(
    guice
  )
}
