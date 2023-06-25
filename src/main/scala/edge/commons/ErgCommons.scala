package commons

import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.Parameters

object ErgCommons {
  val MinBoxFee: Long = Parameters.MinFee
  val MinMinerFee: Long = Parameters.MinFee
  val InfiniteBoxValue: Long = Long.MaxValue
  val minValuePerByte: Int = 360
  val FourKbBoxFee: Long = ErgoBox.MaxBoxSize * minValuePerByte

  def nanoErgsToErgs(nanoErgAmount: Long): Double = {
    val ergsValue = nanoErgAmount.toDouble / Parameters.OneErg

    ergsValue
  }
}
