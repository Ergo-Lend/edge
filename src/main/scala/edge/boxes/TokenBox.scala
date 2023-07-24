package edge.boxes

import edge.registers.{CollByteRegister, StringRegister}
import io.circe.Json
import edge.json.{ErgoJson, Register, RegisterType}
import org.ergoplatform.sdk.ErgoId

case class TokenBox(
  tokenId: ErgoId,
  R4: StringRegister,
  R5: StringRegister,
  R6: StringRegister,
  R7: CollByteRegister,
  R8: StringRegister,
  R9: StringRegister
) {
  def name: String = R4.str
  def description: String = R5.str
  def decimals: String = R6.str
  def nftType: Array[Byte] = R7.value
  def hashOfFile: String = R8.str
  def linkToArtwork: String = R9.str

  def toJson: Json =
    Json.fromFields(
      List(
        ("name", Json.fromString(name)),
        ("description", Json.fromString(description)),
        ("decimals", Json.fromString(decimals)),
        ("nftType", Json.fromString(nftType.mkString("Array(", ", ", ")"))),
        ("hashOfFile", Json.fromString(hashOfFile)),
        ("linkToArtwork", Json.fromString(linkToArtwork))
      )
    )
}

object TokenBox {

  def from(json: Json): TokenBox = {
    val additionalRegistersJson: Json =
      json.hcursor.downField("additionalRegisters").as[Json].getOrElse(null)

    TokenBox(
      tokenId =
        ErgoId.create(json.hcursor.downField("boxId").as[String].getOrElse("")),
      R4 = new StringRegister(
        ErgoJson
          .getRegister(
            registersJson = additionalRegistersJson,
            register = Register.R4,
            getType = RegisterType.CollByte
          )
          .get
          .asInstanceOf[Array[Byte]]
      ),
      R5 = new StringRegister(
        ErgoJson
          .getRegister(
            registersJson = additionalRegistersJson,
            register = Register.R5,
            getType = RegisterType.CollByte
          )
          .get
          .asInstanceOf[Array[Byte]]
      ),
      R6 = new StringRegister(
        ErgoJson
          .getRegister(
            registersJson = additionalRegistersJson,
            register = Register.R6,
            getType = RegisterType.CollByte
          )
          .get
          .asInstanceOf[Array[Byte]]
      ),
      R7 = new CollByteRegister(
        ErgoJson
          .getRegister(
            registersJson = additionalRegistersJson,
            register = Register.R7,
            getType = RegisterType.CollByte
          )
          .getOrElse(Array.emptyByteArray)
          .asInstanceOf[Array[Byte]]
      ),
      R8 = new StringRegister(
        ErgoJson
          .getRegister(
            registersJson = additionalRegistersJson,
            register = Register.R8,
            getType = RegisterType.CollByte
          )
          .getOrElse(Array.emptyByteArray)
          .asInstanceOf[Array[Byte]]
      ),
      R9 = new StringRegister(
        ErgoJson
          .getRegister(
            registersJson = additionalRegistersJson,
            register = Register.R9,
            getType = RegisterType.CollByte
          )
          .getOrElse(Array.emptyByteArray)
          .asInstanceOf[Array[Byte]]
      )
    )
  }
}
