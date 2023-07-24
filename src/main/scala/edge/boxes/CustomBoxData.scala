package edge.boxes

import org.ergoplatform.appkit.{
  BlockchainContext,
  ErgoContract,
  ErgoValue,
  OutBox,
  UnsignedTransactionBuilder
}
import org.ergoplatform.sdk.ErgoToken

case class CustomBoxData(
  customValue: Option[Long] = None,
  customContract: Option[ErgoContract] = None,
  customTokens: Option[Seq[ErgoToken]] = None,
  customRegs: Option[Seq[ErgoValue[_]]] = None
) {

  def applyData(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder,
    boxWrapper: BoxWrapper
  ): OutBox =
    boxWrapper.getCustomOutBox(
      ctx,
      txB,
      customValue,
      customContract,
      customTokens,
      customRegs
    )

  def getBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox =
    WrappedBox().getCustomOutBox(
      ctx,
      txB,
      customValue,
      customContract,
      customTokens,
      customRegs
    )
}
