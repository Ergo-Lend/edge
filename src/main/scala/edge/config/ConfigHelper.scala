package edge.config

import com.typesafe.config.ConfigFactory
import play.api.Configuration

trait ConfigHelper {
  val config: Configuration = Configuration(ConfigFactory.load())

  /**
    * Read the config and return the value of the key
    *
    * @param key     key to find
    * @param default default value if the key is not found
    * @return value of the key
    */
  def readKey(key: String, default: String = null): String =
    try {
      if (default != null && !config.has(key)) return default
      if (config.has(key)) config.getOptional[String](key).getOrElse(default)
      else throw config.reportError(key, message = s"$key is required.")
    } catch {
      case ex: Throwable =>
        println(ex)
        sys.exit()
    }

  def readNodes(): Seq[String] =
    try {
      val key = "nodes"
      if (config.has(key))
        config.getOptional[Seq[String]](key).getOrElse(Seq()).map { ip =>
          if (!ip.startsWith("http")) "http://" + ip
          else ip
        }
      else throw config.reportError(key, message = s"$key is required.")
    } catch {
      case ex: Throwable =>
        println(ex)
        sys.exit()
    }
}
