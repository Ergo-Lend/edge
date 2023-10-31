package edge.EIP12Elements

import org.ergoplatform.appkit.{ContextVar, ErgoValue, InputBox}
import org.ergoplatform.sdk.ErgoToken
import play.api.libs.json._

import scala.collection.JavaConverters._

case class EIP12UnsignedInput(
  extension: EIP12ContextExtension,
  boxId: EIP12BoxId,
  value: EIP12Value,
  ergoTree: EIP12ErgoTree,
  assets: Seq[EIP12TokenAmount],
  additionalRegisters: Seq[(String, EIP12Constant)],
  creationHeight: Int,
  transactionId: EIP12TxId,
  index: Int
) {

  def toJson(): String =
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12UnsignedInputWrites))

}

object EIP12UnsignedInput extends ToEIP12 {

  def apply(
    inputbox: InputBox,
    context: List[ContextVar]
  ): EIP12UnsignedInput = {

    val extension: EIP12ContextExtension = EIP12ContextExtension(context)
    val boxId: EIP12BoxId = EIP12BoxId(inputbox.getId.toString)
    val value: EIP12Value = EIP12Value(inputbox.getValue.toString)
    val ergoTree: EIP12ErgoTree = EIP12ErgoTree(inputbox.getErgoTree.bytesHex)
    val assets: Seq[EIP12TokenAmount] = toAssets(
      inputbox.getTokens.asScala.toSeq
    )
    val additionalRegisters: Seq[(String, EIP12Constant)] = toRegisters(
      inputbox.getRegisters.asScala.toSeq
    )
    val creationHeight: Int = inputbox.getCreationHeight
    val transactionId: EIP12TxId = EIP12TxId(inputbox.getTransactionId)
    val index: Int = inputbox.getTransactionIndex

    EIP12UnsignedInput(
      extension,
      boxId,
      value,
      ergoTree,
      assets,
      additionalRegisters,
      creationHeight,
      transactionId,
      index
    )

  }
}
