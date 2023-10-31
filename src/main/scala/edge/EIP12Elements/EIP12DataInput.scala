package edge.EIP12Elements

import org.ergoplatform.sdk.ErgoId
import play.api.libs.json._

case class EIP12DataInput(
  boxId: EIP12BoxId
) {

  def toJson(): String =
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12DataInputWrites))
}

object EIP12DataInput {

  def apply(id: ErgoId): EIP12DataInput = {
    val boxid: EIP12BoxId = EIP12BoxId(id.toString)
    EIP12DataInput(boxid)
  }
}
