package edge.EIP12Elements

import play.api.libs.json._

case class EIP12TokenId(tokenId: String) {

  def toJson(): String =
    Json.stringify(Json.toJson(this)(EIP12JsonWriters.eip12TokenIdWrites))
}
