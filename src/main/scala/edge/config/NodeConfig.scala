package edge.config

import org.ergoplatform.appkit.Address

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

object TestAddress extends ConfigHelper {

  lazy val testnetAddress: Address =
    Address.create(readKey(s"service.TESTNET").replaceAll("/$", ""))

  lazy val mainnetAddress: Address =
    Address.create(readKey(s"service.MAINNET").replaceAll("/$", ""))
}
