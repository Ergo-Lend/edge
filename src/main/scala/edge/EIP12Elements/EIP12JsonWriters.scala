package edge.EIP12Elements

import play.api.libs.json._

object EIP12JsonWriters {

  implicit val eip12BoxIdWrites = new Writes[EIP12BoxId] {
    override def writes(o: EIP12BoxId): JsValue = JsString(o.boxId)
  }

  implicit val eip12ValueWrites = new Writes[EIP12Value] {
    override def writes(o: EIP12Value): JsValue = JsString(o.value)
  }

  implicit val eip12ErgoTreeWrites = new Writes[EIP12ErgoTree] {
    override def writes(o: EIP12ErgoTree): JsValue = JsString(o.ergotree)
  }

  implicit val eip12TokenIdWrites = new Writes[EIP12TokenId] {
    override def writes(o: EIP12TokenId): JsValue = JsString(o.tokenId)
  }

  implicit val eip12TokenAmountWrites = new Writes[EIP12TokenAmount] {

    override def writes(o: EIP12TokenAmount): JsValue = Json.obj(
      "tokenId" -> eip12TokenIdWrites.writes(o.tokenId),
      "amount" -> eip12ValueWrites.writes(o.amount)
    )
  }

  implicit val eip12ConstantWrites = new Writes[EIP12Constant] {
    override def writes(o: EIP12Constant): JsValue = JsString(o.constant)
  }

  implicit val eip12AdditionalRegistersWrites =
    new Writes[Seq[(String, EIP12Constant)]] {

      override def writes(o: Seq[(String, EIP12Constant)]): JsValue = {
        val registers =
          o.map(reg => reg._1 -> Json.toJsFieldJsValueWrapper(reg._2))
        Json.obj(registers: _*)
      }
    }

  implicit val eip12TxIdWrites = new Writes[EIP12TxId] {
    override def writes(o: EIP12TxId): JsValue = JsString(o.txId)
  }

  implicit val eip12ContextExtensionWrites = new Writes[EIP12ContextExtension] {

    override def writes(o: EIP12ContextExtension): JsValue = {
      val context =
        o.values.map(cv => cv._1 -> Json.toJsFieldJsValueWrapper(cv._2))
      Json.obj(context: _*)
    }
  }

  implicit val eip12BoxWrites = new Writes[EIP12Box] {

    override def writes(o: EIP12Box): JsValue = Json.obj(
      "boxId" -> eip12BoxIdWrites.writes(o.boxId),
      "value" -> eip12ValueWrites.writes(o.value),
      "ergoTree" -> eip12ErgoTreeWrites.writes(o.ergoTree),
      "assets" -> o.assets.map(ta => eip12TokenAmountWrites.writes(ta)),
      "additionalRegisters" -> eip12AdditionalRegistersWrites.writes(
        o.additionalRegisters
      ),
      "creationHeight" -> o.creationHeight,
      "transactionId" -> eip12TxIdWrites.writes(o.transactionId),
      "index" -> o.index
    )
  }

  implicit val eip12BoxCandidateWrites = new Writes[EIP12BoxCandidate] {

    override def writes(o: EIP12BoxCandidate): JsValue = Json.obj(
      "value" -> eip12ValueWrites.writes(o.value),
      "ergoTree" -> eip12ErgoTreeWrites.writes(o.ergoTree),
      "assets" -> o.assets.map(ta => eip12TokenAmountWrites.writes(ta)),
      "additionalRegisters" -> eip12AdditionalRegistersWrites.writes(
        o.additionalRegisters
      ),
      "creationHeight" -> o.creationHeight
    )
  }

  implicit val eip12DataInputWrites = new Writes[EIP12DataInput] {

    override def writes(o: EIP12DataInput): JsValue = Json.obj(
      "boxId" -> eip12BoxIdWrites.writes(o.boxId),
      "value" -> eip12ValueWrites.writes(o.value),
      "ergoTree" -> eip12ErgoTreeWrites.writes(o.ergoTree),
      "assets" -> o.assets.map(ta => eip12TokenAmountWrites.writes(ta)),
      "additionalRegisters" -> eip12AdditionalRegistersWrites.writes(
        o.additionalRegisters
      ),
      "creationHeight" -> o.creationHeight,
      "transactionId" -> eip12TxIdWrites.writes(o.transactionId),
      "index" -> o.index
    )
  }

  implicit val eip12UnsignedInputWrites = new Writes[EIP12UnsignedInput] {

    override def writes(o: EIP12UnsignedInput): JsValue = Json.obj(
      "extension" -> eip12ContextExtensionWrites.writes(o.extension),
      "boxId" -> eip12BoxIdWrites.writes(o.boxId),
      "value" -> eip12ValueWrites.writes(o.value),
      "ergoTree" -> eip12ErgoTreeWrites.writes(o.ergoTree),
      "assets" -> o.assets.map(ta => eip12TokenAmountWrites.writes(ta)),
      "additionalRegisters" -> eip12AdditionalRegistersWrites.writes(
        o.additionalRegisters
      ),
      "creationHeight" -> o.creationHeight,
      "transactionId" -> eip12TxIdWrites.writes(o.transactionId),
      "index" -> o.index
    )
  }

  implicit val eip12Tx = new Writes[EIP12Tx] {

    override def writes(o: EIP12Tx): JsValue = Json.obj(
      "inputs" -> o.inputs.map(in => eip12UnsignedInputWrites.writes(in)),
      "dataInputs" -> o.dataInputs.map(di => eip12DataInputWrites.writes(di)),
      "outputs" -> o.outputs.map(out => eip12BoxCandidateWrites.writes(out))
    )
  }

}
