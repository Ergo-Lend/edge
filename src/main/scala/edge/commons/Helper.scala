package edge.commons

import edge.boxes.FundsToAddressBox
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{Address, InputBox, NetworkType}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}
import sigmastate.Values.ErgoTree

import java.io.{PrintWriter, StringWriter}
import java.util.Calendar
import scala.collection.convert.ImplicitConversions.`iterable AsScalaIterable`

object StackTrace {

  def getStackTraceStr(e: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString
  }
}

object Time {
  def currentTime: Long = Calendar.getInstance().getTimeInMillis / 1000
}

object ErgoValidator {

  def validateErgValue(value: Long): Unit =
    if (value < 10000) throw new Throwable("Minimum value is 0.00001 Erg")

  def validateAddress(
    address: String,
    name: String = "wallet",
    networkType: NetworkType = NetworkType.MAINNET
  ): ErgoTree =
    try {
      val addressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)
      addressEncoder.fromString(address).get.script
    } catch {
      case _: Throwable => throw new Throwable(s"Invalid $name address")
    }

  def validateDeadline(value: Int): Unit =
    if (value < 1) throw new Throwable("Deadline should be positive")
    else if (value > 262800)
      throw new Throwable("Maximum deadline is 262800 blocks (about 1 year)")
}

object ErgoBoxHelper {

  /**
    * Consolidate the boxes that are given so that all of the assets
    * are consolidated into one box for each address
    * We have to consolidate,
    * 1. Value,
    * 2. Tokens
    * for all of the boxes given
    * @param inputBoxes boxes to be consolidated
    * @return
    */
  def consolidateBoxes(inputBoxes: Seq[InputBox]): Seq[FundsToAddressBox] = {
    val inputBoxesByAddress: Map[ErgoTree, Seq[InputBox]] =
      inputBoxes.groupBy(_.getErgoTree)
    val consolidatedBoxes: Seq[FundsToAddressBox] = inputBoxesByAddress.map {
      case (ergoTree, boxes) =>
        // Consolidate Value
        val funderBoxValue: Long = boxes.foldLeft(0L) {
          (acc: Long, inputBox: InputBox) => acc + inputBox.getValue
        }

        // Consolidate tokens
        val funderBoxTokens: Seq[ErgoToken] = {
          val tokenGroups: Map[ErgoId, Seq[ErgoToken]] = boxes
            .flatMap(_.getTokens.toSeq)
            .groupBy(_.getId)

          tokenGroups.map {
            case (tokenId, tokens) =>
              new ErgoToken(tokenId, tokens.map(_.getValue).sum)
          }.toSeq
        }

        FundsToAddressBox(
          value = funderBoxValue,
          tokens = funderBoxTokens,
          address = Address.fromErgoTree(ergoTree, NetworkType.MAINNET)
        )
    }.toSeq

    consolidatedBoxes
  }
}
