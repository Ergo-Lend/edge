package txs

import commons.ErgCommons
import errors.{ProveException, ReducedException}
import boxes.{BoxWrapper, CustomBoxData, WrappedBox}
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  ErgoProver,
  ErgoToken,
  ErgoValue,
  InputBox,
  NetworkType,
  OutBox,
  ReducedTransaction,
  SignedTransaction,
  UnsignedTransaction,
  UnsignedTransactionBuilder
}
import scorex.crypto.hash.Blake2b256
import special.collection.Coll

import scala.collection.JavaConverters.{
  collectionAsScalaIterableConverter,
  seqAsJavaListConverter
}

trait Tx {
  val changeAddress: P2PKAddress
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

  def getOutBoxesAsInputBoxes(txId: String): Seq[InputBox] =
    // Increment number
    getOutBoxes.zipWithIndex.map {
      case (box, count) => box.convertToInputWith(txId, count.toShort)
    }

  def getOutBoxesAsInputBoxesViaDummyTxId: Seq[InputBox] =
    getOutBoxesAsInputBoxes(Tx.dummyTxId)

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
  def visualizeTx: String = {
    val vHead: String = "UNSIGNED TX:" + txInfoStr + "\n"

    val withInputs: String =
      inputBoxes.zipWithIndex.foldLeft(vHead + "INPUTS:") { (z, s) =>
        z + (inputStr(s._1, s._2) + "\n")
      }
    val outBoxesAsInputBoxes: Seq[InputBox] =
      getOutBoxesAsInputBoxesViaDummyTxId

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
        .boxesToSpend(inputBoxes.asJava)
        .outputs(outBoxes: _*)
        .fee(ErgCommons.MinMinerFee)
        .sendChangeTo(changeAddress)

    val txBWithDataInputs: UnsignedTransactionBuilder = dataInputs match {
      case Nil =>
        essentialsTxB
      case _ =>
        essentialsTxB
          .withDataInputs(dataInputs.asJava)
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
        throw ProveException(e.getMessage)
      }
      case _: Throwable => {
        throw new Throwable()
      }
    }

  def signCustomTx(customData: Seq[CustomBoxData]): SignedTransaction =
    sign(buildCustomTx(customData))

  def signTx: SignedTransaction =
    sign(buildTx)

  def signTxWithProver(prover: ErgoProver): SignedTransaction =
    signWithProver(prover, buildTx)

  def reduce(unsignedTx: UnsignedTransaction): ReducedTransaction =
    try {
      ctx.newProverBuilder().build().reduce(unsignedTx, 0)
    } catch {
      case e: Throwable =>
        throw ReducedException(e.getMessage)
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
