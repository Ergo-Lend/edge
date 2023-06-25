package edge.EIP12Elements

import org.ergoplatform.appkit.ContextVar
import play.api.libs.json._

case class EIP12ContextExtension(values: Seq[(String, String)]) {

  def toJson(): String =
    Json.stringify(
      Json.toJson(this)(EIP12JsonWriters.eip12ContextExtensionWrites)
    )

}

object EIP12ContextExtension {

  def apply(extension: List[ContextVar]): EIP12ContextExtension = {

    val context: Seq[(String, String)] = Seq()
    extension.foreach { ext =>
      val elem = (ext.getId.toHexString, ext.getValue.toHex)
      context :+ elem
    }

    EIP12ContextExtension(context)

  }

}
