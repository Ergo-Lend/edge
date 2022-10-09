package node

import commons.ErgCommons
import errors.ConnectionException
import org.ergoplatform.appkit.BoxOperations.ExplorerApiUnspentLoader
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  BoxOperations,
  CoveringBoxes,
  ErgoClient,
  ErgoToken,
  InputBox,
  NetworkType,
  RestApiErgoClient
}
import play.api.Logger

import scala.collection.JavaConverters._

class BaseClient(nodeInfo: NodeInfo) {
  private val logger: Logger = Logger(this.getClass)
  private var client: ErgoClient = _

  def setClient(): Unit = {
    println("Ergo Client Starting up...")
    println(s"Node Url: ${nodeInfo.nodeUrl}")
    try {
      client = RestApiErgoClient.create(
        nodeInfo.nodeUrl,
        nodeInfo.getNetworkType,
        nodeInfo.getApiKey,
        nodeInfo.explorerUrl
      )
      client.execute { ctx =>
        System.out.println(
          s"Client Instantiated, Current Height: ${ctx.getHeight} " +
            s"Network: ${nodeInfo.getNetworkType}"
        )
        ctx.getHeight
      }
      ()
    } catch {
      case e: Throwable =>
        logger.error(message = s"Could not set client! ${e.getMessage}.")
    }
  }

  def getClient: ErgoClient =
    client

  def getContext: BlockchainContext =
    client.execute(ctx => ctx)

  /**
    * @return current height of the blockchain
    */
  def getHeight: Long =
    try {
      client.execute(ctx => ctx.getHeight.toLong)
    } catch {
      case _: Throwable => throw ConnectionException()
    }

  def getAllUnspentBox(address: Address): List[InputBox] =
    client.execute(ctx =>
      try {
        val nullToken: java.util.List[ErgoToken] = List.empty[ErgoToken].asJava
        val inputBoxesLoader = new ExplorerApiUnspentLoader()

        inputBoxesLoader.prepare(ctx, List(address).asJava, 0, nullToken)
        val unspent = BoxOperations.getCoveringBoxesFor(
          (1e9 * 1e8).toLong,
          nullToken,
          false,
          (page: Integer) => inputBoxesLoader.loadBoxesPage(ctx, address, page)
        )

        unspent.getBoxes.asScala.toList
      } catch {
        case e: Throwable =>
          throw ConnectionException(e.getMessage)
      }
    )

  def getCoveringBoxesFor(address: Address, amount: Long): CoveringBoxes =
    client.execute(ctx =>
      try {
        val amountMinusMinerFee: Long = amount - ErgCommons.MinMinerFee
        val boxOperations = BoxOperations.createForSender(address, ctx)
        val inputBoxList =
          boxOperations.withAmountToSpend(amountMinusMinerFee).loadTop()

        val coveringBoxes = new CoveringBoxes(amount, inputBoxList, null, false)

        coveringBoxes
      } catch {
        case _: Throwable => throw ConnectionException()
      }
    )

  def getCoveringBoxesFor(
    address: Address,
    amount: Long,
    tokensToSpend: java.util.List[ErgoToken]
  ): List[InputBox] =
    client.execute(ctx =>
      try {
        val amountMinusMinerFee: Long = amount - ErgCommons.MinMinerFee
        val boxOperations = BoxOperations.createForSender(address, ctx)
        val coveringBoxes = boxOperations
          .withAmountToSpend(amountMinusMinerFee)
          .withTokensToSpend(tokensToSpend)
          .loadTop()

        coveringBoxes.asScala.toList
      } catch {
        case _: Throwable => throw ConnectionException()
      }
    )
}
