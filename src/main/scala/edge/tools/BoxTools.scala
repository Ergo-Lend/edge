package edge.tools

import edge.boxes.{BoxWrapper, FundsToAddressBox}
import edge.commons.ErgCommons
import edge.node.BaseClient
import edge.txs.{BurnTokenTx, Tx}
import edge.utils.ContractUtils
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  Eip4Token,
  ErgoProver,
  InputBox,
  OutBox,
  Parameters,
  ReducedTransaction,
  SignedTransaction,
  UnsignedTransactionBuilder
}
import org.ergoplatform.appkit.config.{ErgoNodeConfig, ErgoToolConfig}
import org.ergoplatform.sdk.{ErgoToken, SecretString}

import java.util.Base64
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.CollectionConverters.SeqHasAsJava

object BoxTools {

  def getProver(nodeConfig: ErgoNodeConfig, config: ErgoToolConfig)(
    implicit ctx: BlockchainContext
  ): ErgoProver =
    ctx
      .newProverBuilder()
      .withMnemonic(
        SecretString.create(nodeConfig.getWallet.getMnemonic),
        SecretString.create(""),
        false
      )
      .withEip3Secret(config.getParameters.get("addressIndex").toInt)
      .build()

  def send(
    ergAmount: Long,
    toAddress: Address,
    tokens: Seq[ErgoToken] = Seq()
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val ownerAddress: Address = prover.getEip3Addresses.get(0)
      val fundBox: Seq[InputBox] = {
        if (tokens.isEmpty) {
          client
            .getCoveringBoxesFor(ownerAddress, ergAmount)
            .getBoxes
            .asScala
            .toSeq
        } else {
          client
            .getCoveringBoxesFor(ownerAddress, ergAmount, tokens.asJava)
        }
      }

      val toFundAddressBox: FundsToAddressBox = FundsToAddressBox(
        address = toAddress,
        value = ergAmount,
        tokens = tokens
      )

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

      val tx = txB
        .addInputs(fundBox: _*)
        .addOutputs(toFundAddressBox.getOutBox(ctx, txB))
        .fee(Parameters.MinFee)
        .sendChangeTo(ownerAddress)
        .build()

      val signed: SignedTransaction = prover.sign(tx)

      Seq(signed)
    }

  def mintTokens(
    tokenName: String,
    tokenDesc: String,
    amount: Long = 1L,
    decimals: Int = 0,
    boxesToSpend: Seq[InputBox] = Seq.empty
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)

      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val directBox: Seq[InputBox] = if (boxesToSpend.isEmpty) {
        client
          .getCoveringBoxesFor(ownerAddress, ErgCommons.MinMinerFee * 10)
          .getBoxes
          .asScala
          .toSeq
      } else boxesToSpend

      val txB: UnsignedTransactionBuilder = ctx.newTxBuilder()

      def eip4Token: Eip4Token = new Eip4Token(
        directBox.head.getId.toString,
        amount,
        tokenName,
        tokenDesc,
        decimals
      )

      val tokenBox: OutBox = txB
        .outBoxBuilder()
        .value(ErgCommons.MinBoxFee)
        .mintToken(eip4Token)
        .contract(ContractUtils.sendToPK(ownerAddress))
        .build()

      val tx = txB
        .addInputs(directBox: _*)
        .addOutputs(tokenBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(ownerAddress)
        .build()

      val signed: SignedTransaction = prover.sign(tx)

      Seq(signed)
    }

  def mergeBox(tokensToMerge: Seq[ErgoToken], outBoxes: Seq[BoxWrapper])(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val ownerAddress: Address = Address.createEip3Address(
        0,
        nodeConfig.getNetworkType,
        SecretString.create(nodeConfig.getWallet.getMnemonic),
        SecretString.create(""),
        false
      )

      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val spendingBoxes =
        ctx.getDataSource.getUnspentBoxesFor(ownerAddress, 0, 500).asScala.toSeq

      val spendingBoxesWithTokens: Seq[InputBox] = spendingBoxes
        .filter(!_.getTokens.isEmpty)

      // Put the boxes with the seqId tokens tokens together in a sequence
      val boxesToMerge: Seq[InputBox] = spendingBoxesWithTokens.filter {
        spendingBox =>
          val hasTokens: Boolean =
            spendingBox.getTokens.asScala.toSeq.exists(token =>
              tokensToMerge.exists(_.getId.equals(token.getId))
            )
          hasTokens
      }.toSeq

      val totalErgsFromTokenBoxes: Long =
        boxesToMerge.foldLeft(0L)((acc, box) => acc + box.getValue)

      val outBoxTotalErgs: Long =
        outBoxes.foldLeft(0L)((acc, box) => acc + box.value) + Parameters.MinFee

      var tx: Tx = null
      if (totalErgsFromTokenBoxes > outBoxTotalErgs) {
        // Merge the boxes together into the box expected.
        tx = Tx(
          inputBoxes = boxesToMerge.toSeq,
          changeAddress = ownerAddress,
          outBoxes = outBoxes.toSeq
        )(ctx)
      } else {
        // Get enough ergs
        val boxesWithTokensUnmerged: Seq[InputBox] =
          spendingBoxesWithTokens.filter { spendingBox =>
            val noTokens: Boolean =
              !spendingBox.getTokens.asScala.toSeq.exists(token =>
                tokensToMerge.exists(_.getId.equals(token.getId))
              )
            noTokens
          }.toSeq

        val totalErgsInUnmergedBox: Long =
          boxesWithTokensUnmerged.foldLeft(0L)((acc, box) => acc + box.getValue)

        val spendingBoxesWithoutTokens: Seq[InputBox] = spendingBoxes
          .filter(_.getTokens.isEmpty)

        var currentTotal: Long =
          totalErgsInUnmergedBox + totalErgsFromTokenBoxes

        val enoughTokenBoxes: Seq[InputBox] =
          spendingBoxesWithoutTokens.flatMap { box =>
            if (currentTotal < outBoxTotalErgs) {
              currentTotal += box.getValue
              Option(box)
            } else {
              None
            }
          } ++ boxesWithTokensUnmerged ++ boxesToMerge

        // Merge the boxes together into the box expected.
        tx = Tx(
          inputBoxes = enoughTokenBoxes.toSeq,
          changeAddress = ownerAddress,
          outBoxes = outBoxes.toSeq
        )(ctx)
      }

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }

  def signReducedTx(
    reducedTxBytesStrings: Seq[String]
  )(client: BaseClient, config: ErgoToolConfig, nodeConfig: ErgoNodeConfig) = {
    val prover = getProver(nodeConfig, config)(client.getContext)

    val txsParsed: Seq[ReducedTransaction] = reducedTxBytesStrings.map { tx =>
      println("Parsing")
      val result =
        client.getContext.parseReducedTransaction(
          Base64.getUrlDecoder.decode(tx)
        )
      println("Parsing Success")
      result
    }

    val signed: Seq[SignedTransaction] = {
      txsParsed.map { tx =>
        val signTx = prover.signReduced(tx, 0)
        println(s"signed ${tx.getId}")
        signTx
      }
    }

    signed
  }

  def mutate(
    boxIdToMutate: String,
    outBoxes: Seq[BoxWrapper],
    dataInputs: Seq[InputBox] = Seq(),
    inputBoxes: Seq[InputBox] = Seq()
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val boxesToMutate: Seq[InputBox] = ctx.getBoxesById(boxIdToMutate)

      val tx: Tx = Tx(
        inputBoxes = boxesToMutate ++ inputBoxes,
        outBoxes = outBoxes,
        changeAddress = ownerAddress,
        dataInputs = dataInputs
      )(ctx)

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }

  def consolidateBoxes(
    ergValue: Long,
    tokens: Seq[ErgoToken]
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val coveringBoxes: List[InputBox] = client
        .getCoveringBoxesFor(
          ownerAddress,
          ergValue + Parameters.MinFee,
          tokens.toList.asJava
        )

      val consolidatedBox: FundsToAddressBox = FundsToAddressBox(
        address = ownerAddress,
        value = ergValue,
        tokens = tokens
      )

      val tx: Tx = Tx(
        inputBoxes = coveringBoxes,
        outBoxes = Seq(consolidatedBox),
        changeAddress = ownerAddress
      )(ctx)

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }

  def burnTokens(
    tokensToBurn: Seq[ErgoToken]
  )(
    client: BaseClient,
    config: ErgoToolConfig,
    nodeConfig: ErgoNodeConfig
  ): Seq[SignedTransaction] =
    client.getClient.execute { ctx =>
      val prover: ErgoProver = getProver(nodeConfig, config)(ctx)
      val ownerAddress: Address = prover.getEip3Addresses.get(0)

      val coveringBoxes: List[InputBox] = client
        .getCoveringBoxesFor(
          ownerAddress,
          Parameters.MinFee * 2,
          tokensToBurn.toList.asJava
        )

      val tx: BurnTokenTx = BurnTokenTx(
        inputBoxes = coveringBoxes,
        changeAddress = ownerAddress,
        tokensToBurn = tokensToBurn
      )(ctx)

      val signed: SignedTransaction = tx.signTxWithProver(prover)

      Seq(signed)
    }

}
