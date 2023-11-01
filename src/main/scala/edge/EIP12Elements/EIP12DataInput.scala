package edge.EIP12Elements

import org.ergoplatform.appkit.InputBox
import org.ergoplatform.sdk.ErgoId
import play.api.libs.json._

import scala.jdk.CollectionConverters.CollectionHasAsScala

case class EIP12DataInput(
  boxId: EIP12BoxId,
  value: EIP12Value,
  ergoTree: EIP12ErgoTree,
  assets: Seq[EIP12TokenAmount],
  additionalRegisters: Seq[(String, EIP12Constant)],
  creationHeight: Int,
  index: Int,
  transactionId: EIP12TxId
) {

  def toJson(): String =
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12DataInputWrites))
}

object EIP12DataInput extends ToEIP12 {

  def apply(inputBox: InputBox): EIP12DataInput = {
    val boxid: EIP12BoxId = EIP12BoxId(inputBox.getId.toString)
    val value: EIP12Value = EIP12Value(inputBox.getValue.toString)
    val ergoTree: EIP12ErgoTree = EIP12ErgoTree(inputBox.getErgoTree.bytesHex)
    val assets: Seq[EIP12TokenAmount] = toAssets(
      inputBox.getTokens.asScala.toSeq
    )
    val additionalRegisters: Seq[(String, EIP12Constant)] = toRegisters(
      inputBox.getRegisters.asScala.toSeq
    )
    val creationHeight: Int = inputBox.getCreationHeight
    val transactionId: EIP12TxId = EIP12TxId(inputBox.getTransactionId)
    val index: Int = inputBox.getTransactionIndex

    EIP12DataInput(
      boxId = boxid,
      value = value,
      ergoTree = ergoTree,
      assets = assets,
      additionalRegisters = additionalRegisters,
      creationHeight = creationHeight,
      transactionId = transactionId,
      index = index
    )
  }
}
