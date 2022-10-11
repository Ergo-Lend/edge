package edge.registers

import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.appkit.{Address, NetworkType}
import registers.Register
import sigmastate.serialization.ErgoTreeSerializer
import special.collection.Coll

import java.nio.charset.StandardCharsets

class NumbersRegister(override val value: Array[Long]) extends Register(value) {}

class StringRegister(val str: String)
    extends Register(CollByte.stringToCollByte(str)) {
  def this(bytes: Array[Byte]) = this(
    new String(bytes, StandardCharsets.UTF_8)
  )

  def this(collByte: Coll[Byte]) = this(
    new String(collByte.toArray, StandardCharsets.UTF_8)
  )
}

class CollStringRegister(val collStr: Array[String]) extends Register(
  collStr
) {
  def this(arrayCollByte: Array[Coll[Byte]]) = this(
    arrayCollByte.map(collByte => new String(collByte.toArray, StandardCharsets.UTF_8))
  )
}

object CollStringRegister {
  def empty: CollStringRegister = {
    new CollStringRegister(Array[String]())
  }
}

class CollByteRegister(override val value: Array[Byte])
    extends Register(value) {

  def getString: String = new String(value, StandardCharsets.UTF_8)
}

class AddressRegister(val address: String)
    extends Register(
      if (address.nonEmpty) Address.create(address).getErgoAddress.script.bytes
      else address.getBytes
    ) {

  def this(arrayByte: Array[Byte]) = this(
    new String(arrayByte, StandardCharsets.UTF_8)
  )

  def this(address: Address) = this(
    address.toString
  )

  def getAddress: Address = Address.create(address)
  def isEmpty: Boolean =
    address.isEmpty
}

object AddressRegister {

  def getAddress(addressBytes: Array[Byte])(implicit networkType: NetworkType): ErgoAddress = {
    val ergoTree =
      ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(addressBytes)
    val encoder = new ErgoAddressEncoder(networkType.networkPrefix)
    encoder.fromProposition(ergoTree).get
  }
}

object CollByte {

  /**
    * Turns a Coll[Byte] to a string
    *
    * @param collByte
    * @return
    */
  def collByteToString(collByte: Coll[Byte]): String =
    new String(collByte.toArray, StandardCharsets.UTF_8)

  def arrayByteToString(arrayByte: Array[Byte]): String =
    new String(arrayByte, StandardCharsets.UTF_8)

  def stringToCollByte(str: String): Array[Byte] =
    str.getBytes("utf-8")
}
