package edge.EIP12Elements

import org.ergoplatform.appkit.{ErgoValue, OutBox}
import org.ergoplatform.sdk.ErgoToken
import play.api.libs.json._

import scala.jdk.CollectionConverters.CollectionHasAsScala

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

object EIP12BoxCandidate extends ToEIP12 {

  def apply(outbox: OutBox): EIP12BoxCandidate = {

    val value: EIP12Value = EIP12Value(outbox.getValue.toString)
    val ergoTree: EIP12ErgoTree = EIP12ErgoTree(outbox.getErgoTree.bytesHex)
    val assets: Seq[EIP12TokenAmount] = toAssets(
      outbox.getTokens.asScala.toSeq
    )
    val additionalRegisters: Seq[(String, EIP12Constant)] = toRegisters(
      outbox.getRegisters.asScala.toSeq
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
}

trait ToEIP12 {

  def toAssets(tokens: Seq[ErgoToken]): Seq[EIP12TokenAmount] = {

    val eip12Tokens: Seq[EIP12TokenAmount] = tokens.map { token =>
      val id = EIP12TokenId(token.getId.toString)
      val value = EIP12Value(token.getValue.toString)
      EIP12TokenAmount(id, value)
    }

    eip12Tokens
  }

  def toRegisters(
    registers: Seq[ErgoValue[_]]
  ): Seq[(String, EIP12Constant)] = {
    val eip12Registers: Seq[(String, EIP12Constant)] =
      registers.zipWithIndex.map {
        case (elem, index) =>
          val constant = EIP12Constant(elem.toHex)
          val registerName = "R" + (index + 4)
          (registerName, constant)
      }

    eip12Registers
  }
}
