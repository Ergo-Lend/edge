package boxes

import org.ergoplatform.appkit.{
  BlockchainContext,
  ErgoContract,
  ErgoToken,
  ErgoValue,
  OutBox,
  UnsignedTransactionBuilder
}

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
}
