package edge.json

import io.circe._
import io.circe.generic.semiauto._

case class Asset(tokenId: String, amount: Long)
case class AdditionalRegisters(R4: String, R5: String, R6: String, R7: String, R8: String, R9: String)
case class BoxData(
                    boxId: String,
                    value: Long,
                    ergoTree: String,
                    assets: List[Asset],
                    creationHeight: Long,
                    additionalRegisters: AdditionalRegisters,
                    transactionId: String,
                    index: Int
                  )

object BoxData {
  implicit val assetDecoder: Decoder[Asset] = deriveDecoder[Asset]
  implicit val additionalRegistersDecoder: Decoder[AdditionalRegisters] = deriveDecoder[AdditionalRegisters]
  implicit val boxDataDecoder: Decoder[BoxData] = deriveDecoder[BoxData]
}
