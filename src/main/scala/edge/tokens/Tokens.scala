package edge.tokens

import edge.config.ConfigHelper
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

trait Token {
  val id: String
  val value: Long

  def toErgoToken: ErgoToken =
    new ErgoToken(id, value)
}

object Tokens extends ConfigHelper {
  lazy val sigUSD: String = readKey("tokens.sigusd")
}

case class SigUSD(override val value: Long) extends Token {
  override val id: String = Tokens.sigUSD
}

object SigUSD {
  val id: ErgoId = ErgoId.create(Tokens.sigUSD)
}

object TokenHelper {

  def applyFunctionToToken(token: ErgoToken, tokenId: ErgoId)(
    function: Long => Long
  ): ErgoToken =
    if (token.getId.equals(tokenId)) {
      new ErgoToken(token.getId, function(token.getValue))
    } else {
      token
    }

  def decrement(token: ErgoToken, tokenId: ErgoId): ErgoToken =
    applyFunctionToToken(token, tokenId)(x => x - 1)

  def increment(token: ErgoToken, tokenId: ErgoId): ErgoToken =
    applyFunctionToToken(token, tokenId)(x => x + 1)
}
