package boxes

import commons.ErgCommons
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoContract,
  ErgoId,
  ErgoToken,
  ErgoValue,
  InputBox,
  NetworkType,
  OutBox,
  UnsignedTransactionBuilder
}
import utils.ContractUtils

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

case class FundsToAddressBox(
  address: Address,
  value: Long = ErgCommons.MinBoxFee,
  override val id: ErgoId = ErgoId.create(""),
  override val tokens: Seq[ErgoToken] = Seq.empty,
  override val box: Option[Box] = Option(null)
) extends BoxWrapper {

  def this(inputBox: InputBox) = this(
    address = Address.fromErgoTree(inputBox.getErgoTree, NetworkType.MAINNET),
    value = inputBox.getValue,
    id = inputBox.getId,
    tokens = inputBox.getTokens.asScala.toSeq,
    box = Option(Box(inputBox))
  )

  /**
    * Get Outbox returns the immediate Outbox of the wrapper.
    * This means it does not go through any modification
    *
    * @param ctx Blockchain Context
    * @param txB TxBuilder
    * @return
    */
  override def getOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox =
    if (tokens.isEmpty) {
      txB
        .outBoxBuilder()
        .value(value)
        .contract(getContract(ctx))
        .build()
    } else {
      txB
        .outBoxBuilder()
        .value(value)
        .tokens(tokens: _*)
        .contract(getContract(ctx))
        .build()
    }

  override def getCustomOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder,
    customValue: Option[Long] = None,
    customContract: Option[ErgoContract] = None,
    customTokens: Option[Seq[ErgoToken]] = None,
    customRegs: Option[Seq[ErgoValue[_]]] = None
  ): OutBox =
    txB
      .outBoxBuilder()
      .value(customValue.getOrElse(value))
      .contract(customContract.getOrElse(getContract(ctx)))
      .tokens(customTokens.getOrElse(tokens): _*)
      .registers(
        customRegs.getOrElse(
          Seq()
        ): _*
      )
      .build()

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    ContractUtils.sendToPK(address)
}
