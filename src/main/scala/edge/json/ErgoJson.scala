package json

import errors.ParseException
import io.circe.{Json => ciJson}
import json.Register.Register
import org.ergoplatform.appkit.{ErgoId, ErgoToken, ErgoValue}
import special.collection.Coll

object ErgoJson {
  import RegisterType._

  def getBoxIds(json: ciJson): Seq[String] = {
    val itemList: List[ciJson] = json.hcursor
      .downField("items")
      .as[List[ciJson]]
      .getOrElse(throw ParseException())

    itemList.map(json => json.hcursor.downField("boxId").as[String].getOrElse("")).toSeq
  }

  def getBoxId(json: ciJson): String =
    json.hcursor
      .downField("items")
      .as[List[ciJson]]
      .getOrElse(throw ParseException())
      .head
      .hcursor
      .downField("boxId")
      .as[String]
      .getOrElse("")

  def getTokens(json: ciJson): Option[Seq[ErgoToken]] = {
    val tokensJson: Option[Seq[ciJson]] = Option(
      json.hcursor.downField("assets").as[Seq[ciJson]].getOrElse(null)
    )
    if (tokensJson.isEmpty) {
      Option(Seq.empty)
    } else {
      def getToken(id: ErgoId, value: Long): ErgoToken =
        new ErgoToken(id, value)
      Option(
        tokensJson.get.map(f =>
          getToken(
            ErgoId.create(
              f.hcursor
                .downField("tokenId")
                .as[String]
                .getOrElse("")
            ),
            f.hcursor
              .downField("amount")
              .as[Long]
              .getOrElse(0)
          )
        )
      )
    }
  }

  def getTxsFromMempool(json: ciJson): Option[Seq[ErgoTx]] = {
    val items: List[ciJson] = json.hcursor
      .downField("items")
      .as[List[ciJson]]
      .getOrElse(throw ParseException())
    if (items.isEmpty) Option(Seq.empty)
    else {
      Option(
        items.map(json =>
          ErgoTx(
            id = ErgoId
              .create(json.hcursor.downField("id").as[String].getOrElse("")),
            creationTimeStamp =
              json.hcursor.downField("creationTimeStamp").as[Long].getOrElse(0),
            size = json.hcursor.downField("size").as[Long].getOrElse(0),
            dataInputs = json.hcursor
              .downField("dataInputs")
              .as[List[ciJson]]
              .getOrElse(Seq.empty),
            inputs = json.hcursor
              .downField("inputs")
              .as[List[ciJson]]
              .getOrElse(Seq.empty),
            outputs = json.hcursor
              .downField("outputs")
              .as[List[ciJson]]
              .getOrElse(Seq.empty)
          )
        )
      )
    }
  }

  def getRegister(
    registersJson: ciJson,
    getType: RegisterType,
    register: Register
  ): Option[Any] = {
    val registerJson: Option[ciJson] = Option(
      registersJson.hcursor
        .downField(register.toString)
        .as[ciJson]
        .getOrElse(null)
    )

    if (registerJson.isEmpty) None
    else {
      getType match {
        case CollByte        => getRegisterAsCollByte(registerJson)
        case CollCollByte    => getRegisterAsCollCollByte(registerJson)
        case CollLong        => getRegisterAsCollLong(registerJson)
        case PairIntCollByte => getRegisterAsPairIntCollByte(registerJson)
        case _               => Option.empty
      }
    }
  }

  private def getRegisterAsCollByte(
    registerJson: Option[ciJson]
  ): Option[Array[Byte]] =
    if (registerJson.isEmpty) {
      Option(Array.emptyByteArray)
    } else {
      Option(
        ErgoValue
          .fromHex(getRegisterValue(registerJson.get))
          .getValue
          .asInstanceOf[Coll[Byte]]
          .toArray
      )
    }

  private def getRegisterAsPairIntCollByte(
    registerJson: Option[ciJson]
  ): Option[(Int, Array[Byte])] =
    if (registerJson.isEmpty) {
      Option.empty
    } else {
      val serializedValue: String = getRegisterValue(registerJson.get)
      val value: (Int, Coll[Byte]) =
        ErgoValue
          .fromHex(serializedValue)
          .getValue
          .asInstanceOf[(Int, Coll[Byte])]
      if (value._2.nonEmpty) {
        Option(
          value.copy(_2 = value._2.toArray)
        )
      } else {
        Option(
          (value._1, Array.emptyByteArray)
        )
      }
    }

  private def getRegisterAsCollCollByte(
    registerJson: Option[ciJson]
  ): Option[Array[Coll[Byte]]] =
    if (registerJson.isEmpty) {
      Option.empty
    } else {
      Option(
        ErgoValue
          .fromHex(getRegisterValue(registerJson.get))
          .getValue
          .asInstanceOf[Coll[Coll[Byte]]]
          .toArray
      )
    }

  private def getRegisterAsCollLong(
    registerJson: Option[ciJson]
  ): Option[Array[Long]] =
    if (registerJson.isEmpty) {
      Option.empty
    } else {
      Option(
        ErgoValue
          .fromHex(getRegisterValue(registerJson.get))
          .getValue
          .asInstanceOf[Coll[Long]]
          .toArray
      )
    }

  private def getRegisterValue(registerJson: ciJson): String =
    registerJson.hcursor
      .downField("serializedValue")
      .as[String]
      .getOrElse("")
}

case class ErgoTx(
  id: ErgoId,
  creationTimeStamp: Long,
  inputs: Seq[ciJson],
  outputs: Seq[ciJson],
  dataInputs: Seq[ciJson],
  size: Long
) {}

object Register extends Enumeration {
  type Register = Value
  val R4, R5, R6, R7, R8, R9 = Value
}

object RegisterType extends Enumeration {
  type RegisterType = Value
  val CollByte, CollCollByte, CollLong, PairIntCollByte = Value
}
