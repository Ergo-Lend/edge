package edge.txs

import edge.boxes.BoxWrapper
import edge.errors.ProveException
import edge.explorer.Explorer
import edge.node.BaseClient
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoClientException,
  InputBox
}

import scala.annotation.tailrec

trait TxProcessor {
  val client: BaseClient
  val explorer: Explorer
  def filterConditions(box: InputBox): Boolean = true
  val inputBoxAddress: Address

  lazy val getInputBoxes: Seq[InputBox] = {
    val box: Seq[InputBox] = client
      .getAllUnspentBox(inputBoxAddress)

    box
      .filter(box => filterConditions(box))
  }

  def isInputBoxInMempool(box: InputBox, inputBoxAddress: Address): Boolean =
    explorer.isBoxInMemPool(box.getId.toString, inputBoxAddress)

  def getTx(inputBoxes: Seq[InputBox], dataInputs: Option[Seq[InputBox]])(
    implicit ctx: BlockchainContext
  ): Tx

  // region Loggings
  def logStartProcess(): Unit =
    println(s"Starting ${this.getClass.getName}")

  def logFinishProcess(): Unit =
    println(s"Ending ${this.getClass.getName} \n")
  // endregion
}

trait GenericTxProcessor extends TxProcessor {

  def process()(implicit ctx: BlockchainContext): Unit = {
    logStartProcess()
    val boxes: Seq[InputBox] = getInputBoxes
    println(
      s"Found ${boxes.length} boxes at address: ${inputBoxAddress.toString}"
    )
    processTx(boxes, inputBoxAddress)
    logFinishProcess()
  }

  def processTx(inputBoxes: Seq[InputBox], address: Address)(
    implicit ctx: BlockchainContext
  ): Unit =
    if (inputBoxes.isEmpty) {
      // nothing happens
      return
    } else {
      // Check if box exists in Mempool
      if (isInputBoxInMempool(inputBoxes.head, address)) {
        processTx(inputBoxes.tail, address)
      } else {
        val boxForProcessing: InputBox = inputBoxes.head
        println(s"Processing box with id: ${boxForProcessing.getId}")
        val inputBoxesForSpending: Option[Seq[InputBox]] =
          getSpendingInputBoxesForTx(boxForProcessing)

        if (inputBoxesForSpending.isEmpty) {
          processTx(inputBoxes.tail, address)
        } else {
          val tx = getTx(
            inputBoxesForSpending.get,
            getDataInputsBoxesForTx(boxForProcessing)
          )

          try {
            tx.signTx
            ctx.sendTransaction(tx.signedTx.get)
            println(
              s"${this.getClass.getName} -> sent tx with id: ${tx.signedTx.get.getId}"
            )
          } catch {
            case e: ProveException =>
              println(e.getMessage)
            case e: ErgoClientException =>
              println(e.getMessage)
            case e: Throwable => println(e.getMessage)
          }

          processTx(
            inputBoxes.tail,
            address
          )
        }
      }
    }

  def getSpendingInputBoxesForTx(
    inputBox: InputBox
  ): Option[Seq[InputBox]] = Option.empty

  def getDataInputsBoxesForTx(
    inputBox: InputBox
  ): Option[Seq[InputBox]] = Option.empty
}

trait SingletonDependentTxProcessor extends TxProcessor {

  def process(
    singletonBox: BoxWrapper
  )(implicit ctx: BlockchainContext): BoxWrapper = {
    logStartProcess(singletonBox)

    val boxes: Seq[InputBox] = getInputBoxes
    println(
      s"Found ${boxes.length} boxes at address: ${inputBoxAddress.toString}"
    )

    val outSingletonBox: BoxWrapper =
      processTx(boxes, singletonBox, inputBoxAddress)
    logFinishProcess(outSingletonBox)
    outSingletonBox
  }

  @tailrec
  final def processTx(
    inputBoxes: Seq[InputBox],
    singletonBox: BoxWrapper,
    address: Address
  )(implicit ctx: BlockchainContext): BoxWrapper =
    if (inputBoxes.isEmpty) singletonBox
    else {
      // Check if box exists in Mempool
      if (isInputBoxInMempool(inputBoxes.head, address)) {
        processTx(inputBoxes.tail, singletonBox, address)
      } else {
        val boxForProcessing: InputBox = inputBoxes.head
        println(s"Processing box with id: ${boxForProcessing.getId}")

        val inputBoxesForSpending: Option[Seq[InputBox]] =
          getSpendingInputBoxesForTx(boxForProcessing, singletonBox)

        // If InputBoxes for spending is empty,
        // we move towards the next list of input box.
        // The reason being that sometimes inputboxes,
        // are in mempool, and we don't want to fk those
        // ones up.
        if (inputBoxesForSpending.isEmpty) {
          processTx(inputBoxes.tail, singletonBox, address)
        } else {
          val tx = getTx(
            inputBoxesForSpending.get,
            getDataInputsBoxesForTx(boxForProcessing, singletonBox)
          )

          // We want to eat up the message cause we dont care if it fails
          try {
            tx.signTx
            ctx.sendTransaction(tx.signedTx.get)
            println(
              s"${this.getClass.getName} -> sent tx with id: ${tx.signedTx.get.getId}"
            )
          } catch {
            case e: ProveException =>
              println(e.getMessage)
            case e: ErgoClientException =>
              println(e.getMessage)
            case e: Throwable => println(e.getMessage)
          }

          val outSingletonBox: BoxWrapper = getOutSingletonBox(tx, singletonBox)
          processTx(
            inputBoxes.tail,
            outSingletonBox,
            address
          )
        }
      }
    }

  def getOutSingletonBox(tx: Tx, singletonBox: BoxWrapper): BoxWrapper

  def getWrappedSingletonBox(inputBox: InputBox): BoxWrapper

  def getSpendingInputBoxesForTx(
    inputBox: InputBox,
    singletonBox: BoxWrapper
  ): Option[Seq[InputBox]] = Option.empty

  def getDataInputsBoxesForTx(
    inputBox: InputBox,
    singletonBox: BoxWrapper
  ): Option[Seq[InputBox]] = Option.empty

  // region Loggings
  def logStartProcess(singletonBox: BoxWrapper): Unit =
    println(s"Starting ${this.getClass.getName}")

  def logFinishProcess(singletonBox: BoxWrapper): Unit =
    println(s"Ending ${this.getClass.getName} \n")
  // endregion
}
