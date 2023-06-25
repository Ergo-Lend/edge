package edge.EIP12Elements

import org.ergoplatform.appkit.{ContextVar, UnsignedTransaction}
import play.api.libs.json._

import scala.collection.JavaConverters._

case class EIP12Tx(
  inputs: Seq[EIP12UnsignedInput],
  dataInputs: Seq[EIP12DataInput],
  outputs: Seq[EIP12BoxCandidate]
) {

  def toJson(): String =
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12Tx))

}

object EIP12Tx {

  def apply(tx: UnsignedTransaction, context: List[ContextVar]): EIP12Tx = {
    val inputs: Seq[EIP12UnsignedInput] =
      tx.getInputs.asScala.toSeq.map(input =>
        EIP12UnsignedInput(input, context)
      )
    val dataInputs: Seq[EIP12DataInput] =
      tx.getDataInputs.asScala.map(data => EIP12DataInput(data.getId))
    val outputs: Seq[EIP12BoxCandidate] =
      tx.getOutputs.asScala.map(output => EIP12BoxCandidate(output))

    EIP12Tx(inputs, dataInputs, outputs)

  }
}
