package errors

final case class InvalidRecaptchaException(
  private val message: String = "Invalid recaptcha"
) extends Throwable(message)

final case class InvalidBoxTypeException(
  private val message: String = s"Box Type is not the same as expected boxType"
) extends Throwable(message)

final case class PaymentNotCoveredException(
  private val message: String = "Payment not Covered"
) extends Throwable(message)

final case class FailedTxException(
  private val message: String = "Tx sending failed"
) extends Throwable(message)

final case class ExplorerException(
  private val message: String = "Explorer error"
) extends Throwable(message)

final case class ConnectionException(
  private val message: String = "Network Error"
) extends Throwable(message)

final case class MempoolBoxRetrievalException(
  private val message: String = "Mempool Box Retrieval Exception"
) extends Throwable(message)

final case class ParseException(private val message: String = "Parsing failed")
    extends Throwable(message)

final case class FinishedLendException(
  private val message: String = "raffle finished"
) extends Throwable(message)

final case class SkipException(private val message: String = "skip")
    extends Throwable(message)

final case class ProveException(
  private val message: String = "Tx proving failed",
  val additionalInfo: String = ""
) extends Throwable(
      if (additionalInfo.nonEmpty) s"${message}: ${additionalInfo}" else message
    )

final case class ReducedException(
  private val message: String = "Tx proving failed",
  val additionalInfo: String = ""
) extends Throwable(
      if (additionalInfo.nonEmpty) s"${message}: ${additionalInfo}" else message
    )

final case class IncorrectBoxStateException(
  private val message: String = s"BoxState is incorrect",
  val additionalInfo: String = ""
) extends Throwable(
      if (additionalInfo.nonEmpty) s"${message}: ${additionalInfo}" else message
    )

final case class PaymentAddressException(
  private val message: String = s"Payment Address generation failed",
  val additionalInfo: String = ""
) extends Throwable(
      if (additionalInfo.nonEmpty) s"${message}: ${additionalInfo}" else message
    )

final case class PaymentBoxInfoNotFoundException(
  private val message: String = "Payment box info cannot be found"
) extends Throwable(message)

/**
  * There is an incompatibility of the token
  * @param message
  */
final case class IncompatibleTokenException(
  token: String,
  targetToken: String,
  private val message: String = "Token is incompatible"
) extends Throwable(
      s"Incompatible Token: TargetToken -> $targetToken, TokenReceived -> $token"
    )

final case class TokenDoesNotExistsInBoxException(
  token: String,
  private val message: String = "Token does not exists in Box"
) extends Throwable(
      s"Token does not exists in Box. Token Id: $token"
    )
