package edge.EIP12Elements

import play.api.libs.json._

case class EIP12TokenAmount(
  tokenId: EIP12TokenId,
  amount: EIP12Value
) {

  def toJson(): String =
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12TokenAmountWrites))
}
