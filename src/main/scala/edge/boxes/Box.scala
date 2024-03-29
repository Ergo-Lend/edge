package edge.boxes

import edge.commons.ErgCommons
import edge.contracts.Contract
import io.circe.Json
import org.ergoplatform.appkit._
import edge.registers.Register
import sigmastate.Values
import special.collection.Coll
import edge.txs.Tx
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

/**
  * A pure data model and InputBox wrapper, representing a box on Ergo
  * @param input InputBox to wrap
  */
case class Box(input: InputBox) {

  def R4: Register[_] = new Register(input.getRegisters.get(0).getValue)
  def R5: Register[_] = new Register(input.getRegisters.get(1).getValue)
  def R6: Register[_] = new Register(input.getRegisters.get(2).getValue)
  def R7: Register[_] = new Register(input.getRegisters.get(3).getValue)
  def R8: Register[_] = new Register(input.getRegisters.get(4).getValue)
  def R9: Register[_] = new Register(input.getRegisters.get(5).getValue)

  def tokens: Seq[ErgoToken] = input.getTokens.asScala.toSeq
  def value: Long = input.getValue.longValue()
  def id: ErgoId = input.getId
  def bytes: Array[Byte] = input.getBytes
  def ergoTree: Values.ErgoTree = input.getErgoTree

  def contract(implicit ctx: BlockchainContext): Contract =
    Contract.fromErgoTree(input.getErgoTree)

  def getErgValue: Double =
    (BigDecimal(value) / Parameters.OneErg).doubleValue
}

object Box {

  /**
    * Create a boxes.Box from an OutBox by converting to an InputBox and wrapping
    * @param output OutBox to convert
    * @param txId Transaction id used to convert output
    * @param index Output index used to convert output
    * @return A boxes.Box wrapping the converted output
    */
  def ofOutBox(output: OutBox, txId: String, index: Int): Box =
    Box(output.convertToInputWith(txId, index.shortValue()))
}

abstract class BoxWrapperJson extends BoxWrapper {
  def toJson(): Json
}

abstract class BoxWrapper extends IBoxRegister {
  val id: ErgoId
  val box: Option[Box]
  val tokens: Seq[ErgoToken]
  val value: Long
  val tokenToMint: Eip4Token = null

  def getOutBoxCost(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): Long = {
    val outBoxAsInputBox: InputBox =
      getOutBox(ctx, txB).convertToInputWith(Tx.dummyTxId, 0)
    val cost: Long =
      outBoxAsInputBox.getBytes.length * ErgCommons.minValuePerByte

    if (cost < ErgCommons.MinBoxFee) ErgCommons.MinBoxFee else cost
  }

  /**
    * Get Outbox returns the immediate Outbox of the wrapper.
    * This means it does not go through any modification
    * @param ctx Blockchain Context
    * @param txB TxBuilder
    * @return
    */
  def getOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder
  ): OutBox = {
    val outBoxBuilder: OutBoxBuilder = txB
      .outBoxBuilder()
      .value(value)
      .contract(this.getContract(ctx))

    // Add Tokens
    val builderAddTokens: OutBoxBuilder =
      if (tokens.nonEmpty)
        outBoxBuilder.tokens(tokens: _*)
      else
        outBoxBuilder

    val builderMintToken: OutBoxBuilder =
      if (tokenToMint != null) {
        builderAddTokens.mintToken(tokenToMint)
      } else {
        builderAddTokens
      }

    // Add Registers
    val registersAsErgoValue: Seq[ErgoValue[_]] =
      this.getRegistersAsErgoValue

    val builderAddRegister: OutBoxBuilder =
      if (registersAsErgoValue.nonEmpty)
        builderMintToken.registers(registersAsErgoValue: _*)
      else
        builderMintToken

    builderAddRegister.build()
  }
  def getContract(implicit ctx: BlockchainContext): ErgoContract

  def getCustomOutBox(
    ctx: BlockchainContext,
    txB: UnsignedTransactionBuilder,
    customValue: Option[Long] = None,
    customContract: Option[ErgoContract] = None,
    customTokens: Option[Seq[ErgoToken]] = None,
    customRegs: Option[Seq[ErgoValue[_]]] = None,
    customTokenMint: Option[Eip4Token] = None
  ): OutBox = {
    val outBoxBuilder: OutBoxBuilder = txB
      .outBoxBuilder()
      .value(customValue.getOrElse(value))
      .contract(customContract.getOrElse(this.getContract(ctx)))

    // Add Tokens
    val builderAddTokens: OutBoxBuilder =
      if (customTokens.nonEmpty || tokens.nonEmpty)
        outBoxBuilder
          .tokens(customTokens.getOrElse(tokens): _*)
      else
        outBoxBuilder

    val builderMintTokens: OutBoxBuilder =
      if (customTokenMint.nonEmpty)
        builderAddTokens
          .mintToken(customTokenMint.get)
      else if (tokenToMint != null)
        builderAddTokens
          .mintToken(tokenToMint)
      else
        builderAddTokens

    // Add Registers
    val registersAsErgoValue: Seq[ErgoValue[_]] =
      this.getRegistersAsErgoValue

    val builderAddRegister: OutBoxBuilder =
      if (customRegs.nonEmpty || registersAsErgoValue.nonEmpty)
        builderMintTokens
          .registers(
            customRegs.getOrElse(
              registersAsErgoValue
            ): _*
          )
      else
        builderMintTokens

    builderAddRegister.build()
  }

  def getAsInputBox(
    txB: UnsignedTransactionBuilder = null,
    txId: String = Tx.dummyTxId,
    index: Int = 0
  )(implicit ctx: BlockchainContext): InputBox =
    getOutBox(ctx, if (txB == null) { ctx.newTxBuilder() }
    else txB).convertToInputWith(txId, index.shortValue())
}

trait IBoxRegister {
  def R4: Option[Register[_]] = Option.empty
  def R5: Option[Register[_]] = Option.empty
  def R6: Option[Register[_]] = Option.empty
  def R7: Option[Register[_]] = Option.empty
  def R8: Option[Register[_]] = Option.empty
  def R9: Option[Register[_]] = Option.empty

  def getRegisters: Seq[Register[_]] =
    Vector.empty[Register[_]] ++ R4 ++ R5 ++ R6 ++ R7 ++ R8 ++ R9

  def getRegistersAsErgoValue: Seq[ErgoValue[_]] =
    this.getRegisters.flatMap(register => register.toErgoValue)
}

trait BoxWrapperHelper {
  def from(inputBox: InputBox): BoxWrapper

  def getAsInstanceOfLong(value: Any): Array[java.lang.Long] = {
    try {
      val result = value.asInstanceOf[Coll[Long]].toArray
      val longArray: Array[java.lang.Long] = result.map(long2Long)
      return longArray
    } catch {
      case _: Throwable =>
    }

    try {
      value.asInstanceOf[Coll[java.lang.Long]].toArray
    } catch {
      case _: Throwable =>
        throw new Throwable("No long conversion available")
    }
  }
}

case class WrappedBox(
  tokens: Seq[ErgoToken] = Seq.empty,
  value: Long = 0L,
  contract: ErgoContract = Address.create("4MQyML64GnzMxZgm").toErgoContract,
  id: ErgoId = ErgoId.create(""),
  box: Option[Box] = None,
  override val R4: Option[Register[_]] = None,
  override val R5: Option[Register[_]] = None,
  override val R6: Option[Register[_]] = None,
  override val R7: Option[Register[_]] = None,
  override val R8: Option[Register[_]] = None,
  override val R9: Option[Register[_]] = None
) extends BoxWrapper {

  override def getContract(implicit ctx: BlockchainContext): ErgoContract =
    contract
}
