import common.ErgoTestBase
import edge.EIP12Elements.{EIP12BoxCandidate, EIP12Tx}
import edge.boxes.FundsToAddressBox
import edge.config.TestAddress
import edge.registers.LongRegister
import org.ergoplatform.appkit.{
  ErgoValue,
  UnsignedTransaction,
  UnsignedTransactionBuilder
}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

class EIP12Spec extends ErgoTestBase {
  client.setClient()
  "EIP12BoxCandidate toAssets function" should {
    "create EIP12TokenAmountSeq" in {
      val ergoTokens: Seq[ErgoToken] = Seq(
        ErgoToken.apply(
          ErgoId.create(
            "0fdb7ff8b37479b6eb7aab38d45af2cfeefabbefdc7eebc0348d25dd65bc2c91"
          ),
          100
        ),
        ErgoToken.apply(
          ErgoId.create(
            "b838120235b98397d6cf86bc91e180c13d40f716768b5b6cee324d1e3154d20d"
          ),
          1
        )
      )

      val eip12Tokens = EIP12BoxCandidate.toAssets(ergoTokens)

      assert(eip12Tokens.length == ergoTokens.length)
      for (x <- ergoTokens.indices) {
        val token = ergoTokens(x)
        val eip12Token = eip12Tokens(x)

        assert(token.value == eip12Token.amount.value.toInt)
        assert(token.id.toString() == eip12Token.tokenId.tokenId)
      }
    }
  }

  "EIP12" should {
    "translate tx properly" in {
      client.getClient.execute { ctx =>
        val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()
        val ergoTokens: Seq[ErgoToken] = Seq(
          ErgoToken.apply(
            ErgoId.create(
              "0fdb7ff8b37479b6eb7aab38d45af2cfeefabbefdc7eebc0348d25dd65bc2c91"
            ),
            100
          ),
          ErgoToken.apply(
            ErgoId.create(
              "b838120235b98397d6cf86bc91e180c13d40f716768b5b6cee324d1e3154d20d"
            ),
            1
          )
        )
        val priceRegister = new LongRegister(100L)
        val fundsToAddressBox: FundsToAddressBox = FundsToAddressBox(
          address = TestAddress.mainnetAddress,
          value = minFee * 2,
          tokens = ergoTokens,
          R4 = Option(priceRegister)
        )
        val outFundsToAddressBox: FundsToAddressBox = FundsToAddressBox(
          address = TestAddress.mainnetAddress,
          value = minFee * 2,
          tokens = ergoTokens,
          R4 = Option(priceRegister)
        )

        val unsignedTx: UnsignedTransaction = txB
          .addInputs(fundsToAddressBox.getAsInputBox()(ctx))
          .addOutputs(outFundsToAddressBox.getOutBox(ctx, txB))
          .sendChangeTo(TestAddress.mainnetAddress)
          .build()

        val eip12Tx: EIP12Tx = EIP12Tx.apply(unsignedTx, List())

        assert(true)
      }
    }
  }
}
