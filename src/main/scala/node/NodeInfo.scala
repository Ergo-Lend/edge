package node

import config.{MainNetNodeConfig, TestNetNodeConfig}
import org.ergoplatform.appkit.NetworkType

class NodeExplorerUrlInfo(nodeUrl: String, explorerUrl: String)

case class MainNetNodeExplorerInfo(
  mainnetNodeUrl: String,
  mainnetExplorerUrl: String
) extends NodeExplorerUrlInfo(mainnetNodeUrl, mainnetExplorerUrl)

case class TestNetNodeExplorerInfo(
  testnetNodeUrl: String,
  testnetExplorerUrl: String
) extends NodeExplorerUrlInfo(testnetNodeUrl, testnetExplorerUrl)

class NodeInfo(
  private val mainNetNodeExplorerInfo: MainNetNodeExplorerInfo,
  private val testNetNodeExplorerInfo: TestNetNodeExplorerInfo,
  networkType: NetworkType,
  apiKey: String = ""
) {

  val nodeUrl: String = {
    networkType match {
      case NetworkType.MAINNET => mainNetNodeExplorerInfo.mainnetNodeUrl
      case NetworkType.TESTNET => testNetNodeExplorerInfo.testnetNodeUrl
    }
  }

  val explorerUrl: String = {
    networkType match {
      case NetworkType.MAINNET => mainNetNodeExplorerInfo.mainnetExplorerUrl
      case NetworkType.TESTNET => testNetNodeExplorerInfo.testnetExplorerUrl
    }
  }

  def getNetworkType: NetworkType = networkType
  def getApiKey: String = apiKey
}

case class DefaultNodeInfo(networkType: NetworkType)
    extends NodeInfo(
      mainNetNodeExplorerInfo = MainNetNodeExplorerInfo(
        mainnetNodeUrl = MainNetNodeConfig.nodeUrl,
        mainnetExplorerUrl = MainNetNodeConfig.explorerUrl
      ),
      testNetNodeExplorerInfo = TestNetNodeExplorerInfo(
        testnetNodeUrl = TestNetNodeConfig.nodeUrl,
        testnetExplorerUrl = TestNetNodeConfig.explorerUrl
      ),
      networkType = networkType
    )
