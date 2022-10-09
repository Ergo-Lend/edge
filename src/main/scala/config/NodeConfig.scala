package config

object MainNetNodeConfig extends ConfigHelper {
  lazy val nodeUrl: String = readKey(s"node.MAINNET.url").replaceAll("/$", "")

  lazy val explorerUrl: String =
    readKey(s"explorer.MAINNET.url").replaceAll("/$", "")

  lazy val explorerFront: String =
    readKey(s"explorer.MAINNET.front").replaceAll("/$", "")
}

object TestNetNodeConfig extends ConfigHelper {
  lazy val nodeUrl: String = readKey(s"node.TESTNET.url").replaceAll("/$", "")

  lazy val explorerUrl: String =
    readKey(s"explorer.TESTNET.url").replaceAll("/$", "")

  lazy val explorerFront: String =
    readKey(s"explorer.TESTNET.front").replaceAll("/$", "")
}
