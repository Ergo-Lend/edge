package edge.registers

import io.circe.Json
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.appkit.{Address, NetworkType}
import sigmastate.serialization.ErgoTreeSerializer
import special.collection.Coll
import special.sigma.GroupElement

import java.nio.charset.StandardCharsets

class NumbersRegister(override val value: Array[Long]) extends Register(value)

class CollBytePairRegister(override val value: (Array[Byte], Array[Byte]))
    extends Register(value) {

  def getString: (String, String) =
    (
      new String(value._1, StandardCharsets.UTF_8),
      new String(value._2, StandardCharsets.UTF_8)
    )
}

class LongPairRegister(override val value: (Long, Long)) extends Register(value)

class LongRegister(override val value: Long) extends Register(value)

class IntRegister(override val value: Int) extends Register(value)

class GroupElementRegister(override val value: GroupElement)
    extends Register(value)

class StringRegister(val str: String)
    extends Register(CollByte.stringToCollByte(str)) {
  def this(bytes: Array[Byte]) = this(
    new String(bytes, StandardCharsets.UTF_8)
  )

  def this(collByte: Coll[Byte]) = this(
    new String(collByte.toArray, StandardCharsets.UTF_8)
  )
}

class CollStringRegister(val collStr: Array[String])
    extends Register(
      collStr
    ) {
  def this(arrayCollByte: Array[Coll[Byte]]) = this(
    arrayCollByte.map(collByte =>
      new String(collByte.toArray, StandardCharsets.UTF_8)
    )
  )
}

object CollStringRegister {

  def empty: CollStringRegister =
    new CollStringRegister(Array[String]())
}

class CollAddressRegister(val collAddress: Seq[Address])
    extends Register(
      collAddress.map(addr => addr.getErgoAddress.script.bytes).toArray
    ) {

  def toJson: Json =
    Json.fromFields(
      List(
        (
          "addresses",
          Json.fromValues(
            collAddress.map(address => Json.fromString(address.toString)).toList
          )
        )
      )
    )
}

object CollAddressRegister {

  def empty: CollAddressRegister =
    new CollAddressRegister(Seq.empty)
}

class CollByteRegister(override val value: Array[Byte])
    extends Register(value) {

  def getString: String = new String(value, StandardCharsets.UTF_8)
}

/**
  * Defaults to MAINNET
  * @param address
  */
class AddressRegister(val address: String)
    extends Register(
      if (address.nonEmpty) Address.create(address).getErgoAddress.script.bytes
      else address.getBytes
    ) {

  def this(
    arrayByte: Array[Byte],
    networkType: NetworkType = NetworkType.MAINNET
  ) = this(
    Address.fromPropositionBytes(networkType, arrayByte).toString
  )

  def this(address: Address) = this(
    address.toString
  )

  def getAddress: Address = Address.create(address)

  def isEmpty: Boolean =
    address.isEmpty
}

object AddressRegister {

  def getAddress(
    addressBytes: Array[Byte]
  )(implicit networkType: NetworkType = NetworkType.MAINNET): ErgoAddress = {
    val ergoTree =
      ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(addressBytes)
    val encoder = new ErgoAddressEncoder(networkType.networkPrefix)
    encoder.fromProposition(ergoTree).get
  }

  def getEmpty(
    implicit networkType: NetworkType = NetworkType.MAINNET
  ): AddressRegister =
    new AddressRegister(Array.emptyByteArray, networkType)
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
