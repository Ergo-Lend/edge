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
import registers.Register
import utils.ContractUtils

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

case class FundsToAddressBox(
  address: Address,
  value: Long = ErgCommons.MinBoxFee,
  override val id: ErgoId = ErgoId.create(""),
  override val tokens: Seq[ErgoToken] = Seq.empty,
  override val box: Option[Box] = Option(null),
  override val R4: Option[Register[_]] = None,
  override val R5: Option[Register[_]] = None,
  override val R6: Option[Register[_]] = None,
  override val R7: Option[Register[_]] = None,
  override val R8: Option[Register[_]] = None,
  override val R9: Option[Register[_]] = None
) extends BoxWrapper {

  def this(inputBox: InputBox) = this(
    address = Address.fromErgoTree(inputBox.getErgoTree, NetworkType.MAINNET),
    value = inputBox.getValue,
    id = inputBox.getId,
    tokens = inputBox.getTokens.asScala.toSeq,
    box = Option(Box(inputBox))
  )

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    ContractUtils.sendToPK(address)
}
