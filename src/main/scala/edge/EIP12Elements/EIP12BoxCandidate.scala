package edge.EIP12Elements

import org.ergoplatform.appkit.{ErgoValue, OutBox}
import org.ergoplatform.sdk.ErgoToken
import play.api.libs.json._

import scala.collection.JavaConverters._

case class EIP12BoxCandidate(
  value: EIP12Value,
  ergoTree: EIP12ErgoTree,
  assets: Seq[EIP12TokenAmount],
  additionalRegisters: Seq[(String, EIP12Constant)],
  creationHeight: Int
) {

  def toJson(): String =
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12BoxCandidateWrites))

}

object EIP12BoxCandidate {

  def apply(outbox: OutBox): EIP12BoxCandidate = {

    val value: EIP12Value = EIP12Value(outbox.getValue.toString)
    val ergoTree: EIP12ErgoTree = EIP12ErgoTree(outbox.getErgoTree.bytesHex)
    val assets: Seq[EIP12TokenAmount] = toAssets(
      outbox.getTokens.asScala.toList
    )
    val additionalRegisters: Seq[(String, EIP12Constant)] = toRegisters(
      outbox.getRegisters.asScala.toList
    )
    val creationHeight: Int = outbox.getCreationHeight

    EIP12BoxCandidate(
      value,
      ergoTree,
      assets,
      additionalRegisters,
      creationHeight
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
