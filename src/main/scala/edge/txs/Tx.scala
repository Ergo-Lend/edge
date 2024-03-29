package edge.txs

import edge.commons.{ErgCommons, StackTrace}
import edge.errors.{ProveException, ReducedException}
import edge.boxes.{BoxWrapper, CustomBoxData, FundsToAddressBox}
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoProver,
  ErgoValue,
  InputBox,
  NetworkType,
  OutBox,
  ReducedTransaction,
  SignedTransaction,
  UnsignedTransaction,
  UnsignedTransactionBuilder
}
import org.ergoplatform.sdk.ErgoToken
import scorex.crypto.hash.Blake2b256
import sigmastate.exceptions.InterpreterException
import special.collection.Coll

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

trait TTx {
  val changeAddress: Address
  implicit val ctx: BlockchainContext

  var signedTx: Option[SignedTransaction] = None
  val inputBoxes: Seq[InputBox]
  val dataInputs: Seq[InputBox] = Seq.empty
  val tokensToBurn: Seq[ErgoToken] = Seq.empty
  val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

  def defineOutBoxWrappers: Seq[BoxWrapper]

  def getOutBoxes: Seq[OutBox] =
    defineOutBoxWrappers.map(boxWrapper => boxWrapper.getOutBox(ctx, txB))

  def getCustomOutBoxes(customData: Seq[CustomBoxData]): Seq[OutBox] = {
    val outBoxWrappers: Seq[BoxWrapper] = defineOutBoxWrappers
    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
    customData.zipWithIndex.map {
      case (customBoxData, index) => {
        // If the wrapper is present, we apply the data
        if (outBoxWrappers.isDefinedAt(index)) {
          customBoxData.applyData(ctx, txB, outBoxWrappers(index))
        } else {
          customBoxData.getBox(ctx, txB)
        }
      }
    }
  }

  def getOutBoxesAsInputBoxes(
    txId: String = Tx.dummyTxId,
    outBoxes: Seq[OutBox] = getOutBoxes
  ): Seq[InputBox] =
    // Increment number
    outBoxes.zipWithIndex.map {
      case (box, count) => box.convertToInputWith(txId, count.toShort)
    }

  def getOutBoxesAsInputBoxesViaDummyTxId(
    outBoxes: Seq[OutBox]
  ): Seq[InputBox] =
    getOutBoxesAsInputBoxes(Tx.dummyTxId, outBoxes = outBoxes)

  private def inputStr(
    inputBox: InputBox,
    idx: Int,
    networkType: NetworkType = NetworkType.MAINNET,
    isDataInput: Boolean = false
  ) =
    s"""
       | ${if (isDataInput) "DATA_INPUTS" else "INPUTS"}[${idx}]
       | ===================================================================
       | id: ${inputBox.getId}
       | value: ${fmtErg(inputBox.getValue)}
       | tokens: ${inputBox.getTokens.asScala
         .map(t => s"    ${t.getId} -> ${t.getValue}, \n")
         .mkString("(\n", "", ")")}
       | registers: ${inputBox.getRegisters.asScala.zipWithIndex
         .map(r => s"    R${r._2 + 4} ${fmtReg(r._1)}, \n")
         .mkString("(\n", "", ")")}
       | bytes: ${inputBox.getBytes.length} bytes
       | creation height: ${inputBox.getCreationHeight}
       | address: ${Address.fromErgoTree(inputBox.getErgoTree, networkType)}
       | address hash: ${Hex.toHexString(
         Blake2b256.hash(inputBox.getErgoTree.bytes)
       )}
       | ===================================================================
       |""".stripMargin

  private def outputStr(
    outBoxAsInputBox: InputBox,
    idx: Int,
    networkType: NetworkType = NetworkType.MAINNET
  ) =
    s"""
       | OUTPUTS[${idx}]
       | ===================================================================
       | value: ${fmtErg(outBoxAsInputBox.getValue)}
       | tokens: ${outBoxAsInputBox.getTokens.asScala
         .map(t => s"    ${t.getId} -> ${t.getValue}, \n")
         .mkString("(\n", "", ")")}
       | registers: ${outBoxAsInputBox.getRegisters.asScala.zipWithIndex
         .map(r => s"     R${r._2 + 4} ${fmtReg(r._1)}, \n")
         .mkString("(\n", "", ")")}
       | bytes: ${outBoxAsInputBox.getBytes.length} bytes
       | creation height: ${outBoxAsInputBox.getCreationHeight}
       | address: ${Address.fromErgoTree(
         outBoxAsInputBox.getErgoTree,
         networkType
       )}
       | address hash: ${Hex.toHexString(
         Blake2b256.hash(outBoxAsInputBox.getErgoTree.bytes)
       )}
       | ===================================================================
       |""".stripMargin

  private def fmtReg(ergoValue: ErgoValue[_]) =
    ergoValue.getValue match {
      case _: Coll[_] =>
        val asArr = ergoValue.getValue.asInstanceOf[Coll[_]].toArray
        asArr match {
          case bytes: Array[Byte] =>
            s"CollByte[${Hex.toHexString(bytes)}]"
          case arrColl: Array[Coll[Any]] =>
            val arrMapped = arrColl.map(_.asInstanceOf[Coll[_]].toArray)
            arrMapped
              .map {
                case subBytes: Array[Byte] =>
                  s"CollByte[${Hex.toHexString(subBytes)}]"
                case other: Array[_] =>
                  other.mkString("Coll(", ", ", ")")
              }
              .mkString("Coll(", ", ", ")")
          case _ =>
            asArr.mkString("Coll(", ", ", ")")
        }
      case _ =>
        ergoValue.getValue.toString
    }

  private def fmtErg(value: Long) = {
    val asErg = ErgCommons.nanoErgsToErgs(value)
    s"${asErg} ERG"
  }

  private def txInfoStr =
    s"""
       | Tx Info
       | ===================================================================
       | Height: ${ctx.getHeight}
       | Change Address: ${changeAddress}
       | Fee: ${ErgCommons.nanoErgsToErgs(ErgCommons.MinMinerFee)}
       | ===================================================================
       |""".stripMargin

  /**
    * Helps visualize an un-signed transaction to help with errors that occur during signing
    */
  def visualizeTx: String =
    visualizeTxWithBoxes()

  def visualizeTxWithBoxes(
    inputBoxes: Seq[InputBox] = inputBoxes,
    outBoxes: Seq[OutBox] = getOutBoxes,
    dataInputs: Seq[InputBox] = dataInputs
  ): String = {
    val vHead: String = "UNSIGNED TX:" + txInfoStr + "\n"

    val withInputs: String =
      inputBoxes.zipWithIndex.foldLeft(vHead + "INPUTS:") { (z, s) =>
        z + (inputStr(s._1, s._2) + "\n")
      }

    val outBoxesAsInputBoxes: Seq[InputBox] =
      getOutBoxesAsInputBoxesViaDummyTxId(outBoxes)

    if (dataInputs.nonEmpty) {
      val withDataInputs: String =
        dataInputs.zipWithIndex.foldLeft(withInputs + "DATA_INPUTS:") {
          (z, s) => z + (inputStr(s._1, s._2, isDataInput = true) + "\n")
        }

      val withOutputs: String =
        outBoxesAsInputBoxes.zipWithIndex.foldLeft(withDataInputs + "OUTPUTS:") {
          (z, s) => z + (outputStr(s._1, s._2) + "\n")
        }

      withOutputs
    } else {
      val withOutputs: String = {
        outBoxesAsInputBoxes.zipWithIndex.foldLeft(withInputs + "OUTPUTS:") {
          (z, s) => z + (outputStr(s._1, s._2) + "\n")
        }
      }

      withOutputs
    }
  }

  def buildWithOutboxes(outBoxes: Seq[OutBox]): UnsignedTransaction = {
    val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

    val essentialsTxB: UnsignedTransactionBuilder =
      txB
        .addInputs(inputBoxes: _*)
        .addOutputs(outBoxes: _*)
        .fee(ErgCommons.MinMinerFee)
        .sendChangeTo(changeAddress)

    val txBWithDataInputs: UnsignedTransactionBuilder = dataInputs match {
      case Nil =>
        essentialsTxB
      case _ =>
        essentialsTxB
          .addDataInputs(dataInputs: _*)
    }

    val txBWithTokenBurn: UnsignedTransactionBuilder = tokensToBurn match {
      case Nil =>
        txBWithDataInputs
      case _ =>
        txBWithDataInputs
          .tokensToBurn(tokensToBurn: _*)
    }

    txBWithTokenBurn.build
  }

  def buildCustomTx(customData: Seq[CustomBoxData]): UnsignedTransaction =
    buildWithOutboxes(getCustomOutBoxes(customData))

  def buildTx: UnsignedTransaction = {
    val outBoxes: Seq[OutBox] = getOutBoxes
    buildWithOutboxes(outBoxes)
  }

  def sign(unsignedTx: UnsignedTransaction): SignedTransaction = {
    val prover: ErgoProver = ctx.newProverBuilder().build()

    signWithProver(prover, unsignedTx)
  }

  def signWithProver(
    prover: ErgoProver,
    unsignedTx: UnsignedTransaction
  ): SignedTransaction =
    try {
      val signedTxWithoutOption: SignedTransaction =
        prover.sign(unsignedTx)
      signedTx = Option(signedTxWithoutOption)

      signedTx.get
    } catch {
      case e: Throwable => {
        if (e.getMessage.contains("Script reduced to false")) {
          throw ProveException(e.getMessage)
        } else {
          throw new Throwable(StackTrace.getStackTraceStr(e))
        }
      }
      case e: InterpreterException => {
        if (e.getMessage.contains("Script reduced to false")) {
          throw ProveException(e.getMessage)
        } else {
          throw e
        }
      }
      case _: Throwable => {
        throw new Throwable()
      }
    }

  def signCustomTx(customData: Seq[CustomBoxData]): SignedTransaction =
    sign(buildCustomTx(customData))

  def signCustomTxWithProver(
    prover: ErgoProver,
    customData: Seq[CustomBoxData]
  ): SignedTransaction =
    signWithProver(prover, buildCustomTx(customData))

  def signTx: SignedTransaction =
    sign(buildTx)

  def signTxWithProver(prover: ErgoProver): SignedTransaction =
    signWithProver(prover, buildTx)

  def reduce(unsignedTx: UnsignedTransaction): ReducedTransaction =
    try {
      val unbuiltProverBuilder = ctx.newProverBuilder()
      val prover = unbuiltProverBuilder.build()
      prover.reduce(unsignedTx, 0)
    } catch {
      case e: Throwable =>
        throw ReducedException(StackTrace.getStackTraceStr(e))
    }

  def reduceTx: ReducedTransaction =
    reduce(buildTx)

  def reduceCustomTx(customData: Seq[CustomBoxData]): ReducedTransaction =
    reduce(buildCustomTx(customData))
}

object Tx {

  val dummyTxId: String =
    "ce552663312afc2379a91f803c93e2b10b424f176fbc930055c10def2fd88a5d"
}

case class Tx(
  override val inputBoxes: Seq[InputBox],
  outBoxes: Seq[BoxWrapper],
  override val changeAddress: Address,
  override val dataInputs: Seq[InputBox] = Seq()
)(implicit val ctx: BlockchainContext)
    extends TTx {
  override def defineOutBoxWrappers: Seq[BoxWrapper] = outBoxes
}

case class BurnTokenTx(
  override val inputBoxes: Seq[InputBox],
  override val changeAddress: Address,
  override val tokensToBurn: Seq[ErgoToken]
)(implicit val ctx: BlockchainContext)
    extends TTx {

  override def defineOutBoxWrappers: Seq[BoxWrapper] = {

    def addTokens(inputBoxes: Seq[InputBox]): Seq[ErgoToken] =
      inputBoxes
        .flatMap(box => box.getTokens.toSeq) // Flatten the sequences of ErgoTokens
        .groupBy(_.id) // Group ErgoTokens by id
        .mapValues(_.map(_.value).sum) // Sum the values for each id
        .toSeq // Convert the map back to a sequence of ErgoTokens
        .map { case (id, value) => ErgoToken(id, value) }
    val inputTokens: Seq[ErgoToken] = addTokens(inputBoxes)

    def removeTokens(
      tokens: Seq[ErgoToken],
      tokensToRemove: Seq[ErgoToken]
    ): Seq[ErgoToken] = {
      val tokensToRemoveMap =
        tokensToRemove.groupBy(_.id).mapValues(_.map(_.value).sum)

      tokens
        .groupBy(_.id)
        .flatMap {
          case (id, groupTokens) =>
            val remainingValue =
              groupTokens.map(_.value).sum - tokensToRemoveMap.getOrElse(id, 0L)
            if (remainingValue > 0) Some(ErgoToken(id, remainingValue))
            else None
        }
        .toSeq
    }

    val resultTokens = removeTokens(inputTokens, tokensToBurn)

    Seq(
      new FundsToAddressBox(
        value =
          ((inputBoxes.foldLeft(0L)((acc, box) => acc + box.getValue)) - ErgCommons.MinBoxFee),
        address = changeAddress,
        tokens = resultTokens
      )
    )
  }
}
