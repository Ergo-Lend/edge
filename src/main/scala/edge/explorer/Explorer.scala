package explorer

import commons.StackTrace
import errors.{ConnectionException, ExplorerException, ParseException}
import io.circe.Json
import io.circe.parser.parse
import node.NodeInfo
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  InputBox
}
import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue, Json => playJson}
import scalaj.http.{BaseHttp, HttpConstants}
import sigmastate.serialization.ErgoTreeSerializer
import txs.TxState
import txs.TxState.TxState

import scala.util.{Failure, Success, Try}

abstract class Explorer(nodeInfo: NodeInfo) {
  private val logger: Logger = Logger(this.getClass)

  private val baseUrlV0 = s"${nodeInfo.explorerUrl}/api/v0"
  private val baseUrlV1 = s"${nodeInfo.explorerUrl}/api/v1"
  private val tx = s"$baseUrlV1/transactions"
  private val unconfirmedTx = s"$baseUrlV0/transactions/unconfirmed"

  private val unspentBoxesByTokenId =
    s"$baseUrlV1/boxes/unspent/byTokenId"
  private val boxesP1 = s"$baseUrlV1/boxes"
  private val mempoolTransactions = s"$baseUrlV1/mempool/transactions/byAddress"

  def getTxsInMempoolByAddress(address: String): Json =
    try {
      GetRequest.httpGet(s"$mempoolTransactions/$address")
    } catch {
      case _: Throwable => Json.Null
    }

  def getNumberTxInMempoolByAddress(address: String): Int =
    try {
      val newJson = getTxsInMempoolByAddress(address)
      val js = playJson.parse(newJson.toString())
      (js \ "total").as[Int]
    } catch {
      case _: Throwable => 0
    }

  /**
    * @param txId transaction id
    * @return transaction if it is unconfirmed
    */
  def getUnconfirmedTx(txId: String): Json =
    try {
      GetRequest.httpGet(s"$unconfirmedTx/$txId")
    } catch {
      case _: Throwable => Json.Null
    }

  /**
    * @param txId transaction id
    * @return transaction if it is confirmed (mined)
    */
  def getConfirmedTx(txId: String): Json =
    try {
      GetRequest.httpGet(s"$tx/$txId")
    } catch {
      case _: Throwable => Json.Null
    }

  /**
    * @param txId transaction id
    * @return -1 if tx does not exist, 0 if it is unconfirmed, otherwise, confirmation num
    */
  def getConfirmationNumber(txId: String): Long = {
    val unconfirmedTx = getUnconfirmedTx(txId)
    if (unconfirmedTx != Json.Null) 0
    else {
      val confirmedTx = getConfirmedTx(txId)
      if (confirmedTx != Json.Null)
        confirmedTx.hcursor
          .downField("summary")
          .as[Json]
          .getOrElse(Json.Null)
          .hcursor
          .downField("confirmationsCount")
          .as[Int]
          .getOrElse(-1)
          .toLong
      else -1L
    }
  }

  def getUnspentTokenBoxes(
    tokenId: String,
    offset: Int = 0,
    limit: Int = 100
  ): Json =
    try {
      val url = s"$unspentBoxesByTokenId/$tokenId?offset=$offset&limit=$limit"
      GetRequest.httpGet(
        url
      )
    } catch {
      case _: Throwable => Json.Null
    }

  /**
    * Get Box by Id, spent or unspent
    * @param boxId
    * @return
    */
  def getBoxById(boxId: String): Json =
    try {
      GetRequest.httpGet(s"$boxesP1/$boxId")
    } catch {
      case _: Throwable => Json.Null
    }

  def getUnconfirmedTxByAddress(address: String): Json =
    try {
      GetRequest.httpGet(
        s"$unconfirmedTx/byAddress/$address/?offset=0&limit=100"
      )
    } catch {
      case _: Throwable => Json.Null
    }

  /**
    * Check the transaction state
    * @param txId
    * @return
    */
  def checkTransactionState(txId: String): TxState =
    try {
      if (txId.nonEmpty) {
        val unconfirmedTx = getUnconfirmedTx(txId)

        if (unconfirmedTx == Json.Null) {

          val confirmedTx = getConfirmedTx(txId)
          if (confirmedTx == Json.Null) {
            // Tx unsuccessful
            TxState.Unsuccessful // resend transaction
          } else {
            TxState.Mined // transaction mined
          }

        } else {
          // in mempool but not mined
          TxState.Mempooled // transaction already in mempool
        }
      } else {
        TxState.Unsuccessful
      }
    } catch {
      case e: ExplorerException => {
        logger.warn(e.getMessage)
        throw ConnectionException()
      }
      case e: Throwable => {
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
      }
    }

  def getAddress(addressBytes: Array[Byte]): ErgoAddress = {
    val ergoTree =
      ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree(addressBytes)
    ErgoAddressEncoder(nodeInfo.getNetworkType.networkPrefix)
      .fromProposition(ergoTree)
      .get
  }

  def isBoxInMemPool(box: InputBox): Boolean =
    try {
      val address = getAddress(box.getErgoTree.bytes)
      val transactions =
        playJson.parse(getTxsInMempoolByAddress(address.toString).toString())
      if (transactions != null) {
        (transactions \ "items").as[List[JsValue]].exists { tx =>
          if ((tx \ "inputs")
                .as[JsValue]
                .toString()
                .contains(box.getId.toString)) true
          else false
        }
      } else {
        false
      }
    } catch {
      case e: ExplorerException =>
        logger.warn(e.getMessage)
        throw ConnectionException()
      case e: ParseException =>
        logger.warn(e.getMessage)
        throw ConnectionException()
      case e: JsResultException =>
        logger.warn(e.getMessage)
        throw e
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
    }

  def isBoxInMemPool(boxId: String, address: Address): Boolean =
    try {
      val transactions =
        playJson.parse(getTxsInMempoolByAddress(address.toString).toString())
      if (transactions != null) {
        (transactions \ "items").as[List[JsValue]].exists { tx =>
          if ((tx \ "inputs")
                .as[JsValue]
                .toString()
                .contains(boxId)) true
          else false
        }
      } else {
        false
      }
    } catch {
      case e: ExplorerException =>
        logger.warn(e.getMessage)
        throw ConnectionException()

      case e: ParseException =>
        logger.warn(e.getMessage)
        throw ConnectionException()

      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw new Throwable("Something is wrong")
    }

  def findMempoolBox(
    address: String,
    box: InputBox,
    ctx: BlockchainContext
  ): InputBox =
    try {
      val mempool =
        playJson.parse(getUnconfirmedTxByAddress(address).toString())
      var outBox = box
      val txs = (mempool \ "items").as[List[JsValue]]
      var txMap: Map[String, JsValue] = Map()
      txs.foreach { txJson =>
        val txInput = (txJson \ "inputs").as[List[JsValue]].head
        val id = (txInput \ "id").as[String]
        txMap += (id -> txJson)
      }
      val keys = txMap.keys.toSeq
      logger.debug(outBox.getId.toString)
      while (keys.contains(outBox.getId.toString)) {
        logger.debug(keys.toString())
        val txJson = txMap(outBox.getId.toString)
        val inputs =
          (txJson \ "inputs").as[JsValue].toString().replaceAll("id", "boxId")
        val outputs = (txJson \ "outputs")
          .as[JsValue]
          .toString()
          .replaceAll("id", "boxId")
          .replaceAll("txId", "transactionId")
        val dataInputs = (txJson \ "dataInputs").as[JsValue].toString()
        val id = (txJson \ "id").as[String]
        val newJson =
          s"""{
              "id": "$id",
              "inputs": $inputs,
              "dataInputs": $dataInputs,
              "outputs": $outputs
             }"""
        val tmpTx = ctx.signedTxFromJson(newJson.replaceAll("null", "\"\""))
        outBox = tmpTx.getOutputsToSpend.get(0)
      }
      outBox
    } catch {
      case e: ExplorerException =>
        logger.warn(e.getMessage)
        throw ConnectionException()
      case _: ParseException    => throw ConnectionException()
      case e: JsResultException => throw e
      case e: Throwable =>
        logger.error(StackTrace.getStackTraceStr(e))
        throw e
    }
}

object GetRequest {

  object DotHttp
      extends BaseHttp(
        None,
        HttpConstants.defaultOptions,
        HttpConstants.utf8,
        4096,
        "Mozilla/5.0 (X11; OpenBSD amd64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36",
        true
      )

  private val defaultHeader: Seq[(String, String)] =
    Seq[(String, String)](("Accept", "application/json"))

  def httpGetWithError(
    url: String,
    headers: Seq[(String, String)] = defaultHeader
  ): Either[Throwable, Json] =
    Try {
      val responseReq = DotHttp(url).headers(defaultHeader).asString
      (responseReq.code, responseReq)
    } match {
      case Success((200, responseReq)) => parse(responseReq.body)
      case Success((responseHttpCode, responseReq)) =>
        Left(
          ExplorerException(
            s"returned a error with http code $responseHttpCode and error ${responseReq.throwError}"
          )
        )
      case Failure(exception) => Left(exception)
    }

  def httpGet(url: String): Json =
    httpGetWithError(url) match {
      case Right(json) => json
      case Left(ex)    => throw ex
    }
}
