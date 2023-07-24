import common.ErgoTestBase
import edge.boxes.Box
import org.ergoplatform.appkit.{
  ConstantsBuilder,
  ErgoContract,
  ErgoValue,
  InputBox,
  UnsignedTransactionBuilder
}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}
import special.collection.Coll

import java.math.BigInteger

class BoxSpec extends ErgoTestBase {
  client.setClient()

  val dummyContract: String =
    """
      |{
      |   sigmaProp(true)
      |}
      |""".stripMargin

  val testContract: ErgoContract = client.getClient.execute { ctx =>
    ctx.compileContract(new ConstantsBuilder().build(), dummyContract)
  }

  "Box" should {
    client.getClient.execute { ctx =>
      val r4: ErgoValue[Integer] = ErgoValue.of(123)
      val r5: ErgoValue[Coll[java.lang.Byte]] = ErgoValue.of("hello".getBytes)
      val r6: ErgoValue[java.lang.Long] = ErgoValue.of(123L)
      val r8: ErgoValue[java.lang.Boolean] = ErgoValue.of(true)
      val r9: ErgoValue[java.lang.Short] = ErgoValue.of(123.toShort)

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
      val inputBox: InputBox = txB
        .outBoxBuilder()
        .value(oneErg)
        .registers(
          r4,
          r5,
          r6,
          ErgoValue.of(new BigInteger("123")),
          r8,
          r9
        )
        .tokens(new ErgoToken(new ErgoId("abc".getBytes), 1))
        .contract(testContract)
        .build()
        .convertToInputWith(dummyTxId, 0)

      val box: Box = Box(inputBox)
      val tokens: Seq[ErgoToken] = box.tokens
      val primaryToken: ErgoToken = tokens.head

      "registers populated correctly" in {
        assert(box.R4.toErgoValue.get == r4)
        assert(box.R6.toErgoValue.get == r6)
        assert(box.R8.toErgoValue.get == r8)
        assert(box.R9.toErgoValue.get == r9)
        assert(box.R5.toErgoValue.get == r5)
//        assert(box.R7.toErgoValue.get == ErgoValue.of(new BigInteger("123")))
      }
    }
  }
}
