package edge.EIP12Elements

import org.ergoplatform.appkit.{ContextVar, ErgoToken, ErgoValue, InputBox}
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

object EIP12UnsignedInput {

  def apply(
    inputbox: InputBox,
    context: List[ContextVar]
  ): EIP12UnsignedInput = {

    val extension: EIP12ContextExtension = EIP12ContextExtension(context)
    val boxId: EIP12BoxId = EIP12BoxId(inputbox.getId.toString)
    val value: EIP12Value = EIP12Value(inputbox.getValue.toString)
    val ergoTree: EIP12ErgoTree = EIP12ErgoTree(inputbox.getErgoTree.bytesHex)
    val assets: Seq[EIP12TokenAmount] = toAssets(
      inputbox.getTokens.asScala.toList
    )
    val additionalRegisters: Seq[(String, EIP12Constant)] = toRegisters(
      inputbox.getRegisters.asScala.toList
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
