package edge.EIP12Elements

import org.ergoplatform.appkit.{ContextVar, InputBox, UnsignedTransaction}
import play.api.libs.json._

import scala.collection.JavaConverters._

case class EIP12Tx(
  inputs: Seq[EIP12UnsignedInput],
  dataInputs: Seq[EIP12DataInput],
  outputs: Seq[EIP12BoxCandidate]
) {

  def toJson(): JsValue =
    Json.toJson(this)(EIP12JsonWriters.eip12Tx)

  def toJsonString(): String =
    Json.stringify(this.toJson())

}

object EIP12Tx {

  def apply(tx: UnsignedTransaction, context: Seq[Option[List[ContextVar]]]): EIP12Tx = {
    val inputs: Seq[EIP12UnsignedInput] =
      tx.getInputs.asScala.toSeq.zipWithIndex.map((indexedInputs) =>
        {
          val boxContextVar: List[ContextVar] = if (context.size > indexedInputs._2) {
            context(indexedInputs._2).getOrElse(List())
          } else List()

          EIP12UnsignedInput(
            indexedInputs._1,
            boxContextVar)
        }
      )

    val dataInputs: Seq[EIP12DataInput] =
      tx.getDataInputs.asScala.map(dataInput => EIP12DataInput(dataInput)).toSeq
    val outputs: Seq[EIP12BoxCandidate] =
      tx.getOutputs.asScala.map(output => EIP12BoxCandidate(output)).toSeq

    EIP12Tx(inputs, dataInputs, outputs)

  }
}
