import org.ergoplatform.appkit.{ErgoType, ErgoValue}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import registers.{Register, RegisterTypeException}

import java.math.BigInteger

class RegisterSpec extends AnyWordSpec with Matchers {
  "Registers" should {
    val arrayByteVal: Array[Byte] = "a".getBytes
    val bigIntVal: BigInteger = new BigInteger("123")
    val shortVal: Short = 1.toShort
    val booleanVal: Boolean = true
    val byteVal: Byte = 123

    val intRegister: Register[Int] = new Register(123)
    val longRegister: Register[Long] = new Register(123L)
    val arrayByteRegister: Register[Array[Byte]] = new Register(arrayByteVal)
    val bigIntRegister: Register[BigInteger] = new Register(bigIntVal)
    val shortRegister: Register[Short] = new Register(shortVal)
    val booleanRegister: Register[Boolean] = new Register(booleanVal)
    val byteRegister: Register[Byte] = new Register(byteVal)
    val emptyRegister: Register[Array[Int]] = new Register(Array.emptyIntArray)

    "int val convert to int ergo types" in {
      assert(intRegister.value == 123)
      assert(intRegister.ergoType == ErgoType.integerType())
      assert(intRegister.toErgoValue.get == ErgoValue.of(123))
    }

    "long val convert to long ergo types" in {
      assert(longRegister.value == 123L)
      assert(longRegister.ergoType == ErgoType.longType())
      assert(longRegister.toErgoValue.get == ErgoValue.of(123L))
    }

    "array byte val convert to array byte ergo types" in {
      assert(arrayByteRegister.value.sameElements(arrayByteVal))
      assert(
        arrayByteRegister.ergoType == ErgoType.collType(ErgoType.byteType())
      )
      assert(
        arrayByteRegister.toErgoValue.get == ErgoValue.of(
          arrayByteVal
        )
      )
    }

    "bigInt val convert to bigInt ergo types" in {
      assert(bigIntRegister.value == bigIntVal)
      assert(bigIntRegister.ergoType == ErgoType.bigIntType())
      assert(bigIntRegister.toErgoValue.get == ErgoValue.of(bigIntVal))
    }

    "short val convert to short ergo types" in {
      assert(shortRegister.value == shortVal)
      assert(shortRegister.ergoType == ErgoType.shortType())
      assert(shortRegister.toErgoValue.get == ErgoValue.of(shortVal))
    }

    "boolean val convert to boolean ergo types" in {
      assert(booleanRegister.value == booleanVal)
      assert(booleanRegister.ergoType == ErgoType.booleanType())
      assert(booleanRegister.toErgoValue.get == ErgoValue.of(booleanVal))
    }

    "byte val convert to byte ergo types" in {
      assert(byteRegister.value == byteVal)
      assert(byteRegister.ergoType == ErgoType.byteType())
      assert(byteRegister.toErgoValue.get == ErgoValue.of(byteVal))
    }

    "empty array throws exception" in {
      assert(emptyRegister.value.sameElements(Array.emptyIntArray))
      intercept[RegisterTypeException] {
        emptyRegister.toErgoValue
      }
      intercept[RegisterTypeException] {
        emptyRegister.ergoType
      }
    }
  }
}
