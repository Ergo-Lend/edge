package edge.EIP12Elements

import org.ergoplatform.appkit.{ErgoToken, ErgoValue, OutBox}
import play.api.libs.json._

import scala.collection.JavaConverters._

case class EIP12Box(
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
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12BoxWrites))

}

object EIP12Box {

  def apply(outbox: OutBox, txId: String, outputIndex: Short): EIP12Box = {

    val output = outbox.convertToInputWith(txId, outputIndex)

    val boxId: EIP12BoxId = EIP12BoxId(output.getId.toString)
    val value: EIP12Value = EIP12Value(output.getValue.toString)
    val ergoTree: EIP12ErgoTree = EIP12ErgoTree(output.getErgoTree.bytesHex)
    val assets: Seq[EIP12TokenAmount] = toAssets(
      output.getTokens.asScala.toList
    )
    val additionalRegisters: Seq[(String, EIP12Constant)] = toRegisters(
      output.getRegisters.asScala.toList
    )
    val creationHeight: Int = output.getCreationHeight
    val transactionId: EIP12TxId = EIP12TxId(output.getTransactionId)
    val index: Int = output.getTransactionIndex

    EIP12Box(
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

  def toAssets(tokens: List[ErgoToken]): Seq[EIP12TokenAmount] = {

    val eip12Tokens: Seq[EIP12TokenAmount] = Seq()

    tokens.foreach { t =>
      val id = EIP12TokenId(t.getId.toString)
      val value = EIP12Value(t.getValue.toString)
      val tokenAmount = EIP12TokenAmount(id, value)
      eip12Tokens :+ tokenAmount
    }

    eip12Tokens

  }

  def toRegisters(
    registers: List[ErgoValue[_]]
  ): Seq[(String, EIP12Constant)] = {

    val eip12Registers: Seq[(String, EIP12Constant)] = Seq()

    registers.zipWithIndex.foreach {
      case (elem, index) =>
        val constant = EIP12Constant(elem.toHex)
        val registerName = "R" + index
        eip12Registers :+ (registerName -> constant)

    }
    eip12Registers
  }

}
