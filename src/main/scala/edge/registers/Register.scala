package registers

import org.ergoplatform.appkit.JavaHelpers.JLongRType
import org.ergoplatform.appkit.{ErgoType, ErgoValue, Iso, JavaHelpers}
import sigmastate.Values.SigmaBoolean
import special.collection.Coll
import special.sigma.SigmaProp

import java.math.BigInteger

/**
  * A register value used in the boxes.Box wrapper class.
  * CAUTION: Broken for empty arrays until I find a good fix for type matching
  * @param value Value to be held in the register
  * @tparam T type of underlying value in register
  */
class Register[T](val value: T) extends IRegVal[T] {

  // TODO: Fix typing for empty arrays. Maybe use shapeless?
  override def ergoType: ErgoType[_] =
    value match {
      case i: Int         => ErgoType.integerType()
      case l: Long        => ErgoType.longType()
      case b: Byte        => ErgoType.byteType()
      case bI: BigInteger => ErgoType.bigIntType()
      case sp: SigmaProp  => ErgoType.sigmaPropType()
      case sh: Short      => ErgoType.shortType()
      case bl: Boolean    => ErgoType.booleanType()
      case str: String    => ErgoType.collType(ErgoType.byteType())
      case ars: Array[String] =>
        ErgoType.collType(ErgoType.collType(ErgoType.byteType()))
      case arl: Array[Long] => ErgoType.collType(ErgoType.longType())
      case arb: Array[Byte] => ErgoType.collType(ErgoType.byteType())
      case cb: Coll[Byte]   => ErgoType.collType(ErgoType.byteType())
      case cl: Coll[Long]   => ErgoType.collType(ErgoType.longType())
      case ararb: Array[Array[Byte]] =>
        ErgoType.collType(ErgoType.collType(ErgoType.byteType()))
      case pis: (Int, String) =>
        ErgoType.pairType(
          ErgoType.integerType(),
          ErgoType.collType(ErgoType.byteType())
        )
      case pls: (Long, String) =>
        ErgoType.pairType(
          ErgoType.longType(),
          ErgoType.collType(ErgoType.byteType())
        )
      case _ =>
        RegisterTypeException("Could not determine ErgoType for given RegVal")
    }

  /**
    * Used for converting values to box ready value
    * @return
    */
  override def toErgoValue: Option[ErgoValue[_]] =
    value match {
      case i: Int         => Option(ErgoValue.of(i.asInstanceOf[Int]))
      case l: Long        => Option(ErgoValue.of(l.asInstanceOf[Long]))
      case b: Byte        => Option(ErgoValue.of(b.asInstanceOf[Byte]))
      case bI: BigInteger => Option(ErgoValue.of(bI.asInstanceOf[BigInteger]))
      case sp: SigmaProp  => Option(ErgoValue.of(sp.asInstanceOf[SigmaBoolean]))
      case sh: Short      => Option(ErgoValue.of(sh.asInstanceOf[Short]))
      case bl: Boolean    => Option(ErgoValue.of(bl.asInstanceOf[Boolean]))
      case str: String    => Option(ErgoValue.of(str.getBytes))
      case ars: Array[String] =>
        Option(
          ErgoValue.of(
            ars
              .asInstanceOf[Array[String]]
              .map(str => str.getBytes("utf-8"))
              .map(item => ErgoValue.of(IndexedSeq(item: _*).toArray))
              .map(item => item.getValue),
            ErgoType.collType(ErgoType.byteType())
          )
        )
      case arl: Array[Long] =>
        if (arl.nonEmpty) {
          Option(
            ErgoValue.of(
              JavaHelpers.SigmaDsl.Colls
                .fromArray(
                  arl.asInstanceOf[Array[Long]]
                )
                .map(Iso.jlongToLong.from),
              ErgoType.longType()
            )
          )
        } else Option.empty
      case cb: Coll[Byte] =>
        if (cb.nonEmpty)
          Option(
            ErgoValue.of(
              cb.toArray
            )
          )
        else Option.empty
      case cl: Coll[Long] =>
        if (cl.nonEmpty)
          Option(
            ErgoValue.of(
              JavaHelpers.SigmaDsl.Colls
                .fromArray(
                  cl.toArray
                )
                .map(Iso.jlongToLong.from),
              ErgoType.longType()
            )
          )
        else Option.empty
      case arb: Array[Byte] =>
        if (arb.nonEmpty)
          Option(
            ErgoValue.of(
              arb.asInstanceOf[Array[Byte]]
            )
          )
        else Option.empty
      case ararb: Array[Array[Byte]] =>
        Option(
          ErgoValue.of(
            ararb
              .asInstanceOf[Array[Array[Byte]]]
              .map(item => ErgoValue.of(IndexedSeq(item: _*).toArray))
              .map(item => item.getValue),
            ErgoType.collType(ErgoType.byteType())
          )
        )
      case pis: (Int, String) =>
        Option(
          ErgoValue.pairOf(
            ErgoValue.of(pis._1),
            ErgoValue.of(pis._2.getBytes())
          )
        )
      case pls: (Long, String) =>
        Option(
          ErgoValue.pairOf(
            ErgoValue.of(pls._1),
            ErgoValue.of(pls._2.getBytes())
          )
        )
      case _ =>
        RegisterTypeException("Could not determine ErgoValue for given RegVal")
    }
}

/**
  * Base trait for all register classes
  *
  * @tparam T type of underlying value in register
  */
trait IRegVal[_] {
  def ergoType: ErgoType[_]

  def toErgoValue: Option[ErgoValue[_]]
}
