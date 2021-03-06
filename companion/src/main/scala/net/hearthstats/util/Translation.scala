package net.hearthstats.util

import java.util.ResourceBundle
import scala.util.Try
import java.text.MessageFormat
import java.util.Locale
import grizzled.slf4j.Logging

case class TranslationConfig(bundle: String, language: String) {
  def getBundle: ResourceBundle =
    ResourceBundle.getBundle(bundle, Locale.forLanguageTag(language), new UTF8Control)
}

class Translation(config: TranslationConfig) extends Logging {
  val resourceBundle: Option[ResourceBundle] =
    Try(config.getBundle).toOption

  def t(key: String): String =
    Option(resourceBundle.get.getString(key)) match {
      case Some(value) =>
        value
      case None =>
        warn(s"$key not found in ${config.bundle} for language ${config.language}")
        key
    }

  def t(key: String, args: Any*): String =
    MessageFormat.format(t(key), args.map(_.toString): _*)

  def has(key: String) = resourceBundle match {
    case Some(b) => b.containsKey(key)
    case None => false
  }

  def opt(key: String) =
    if (has(key)) Some(t(key))
    else None
}

/**
 * Support for encoding properties files as UTF-8 instead of the default ISO-8859-1.
 * see http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
 */
class UTF8Control extends ResourceBundle.Control {

  import java.io.IOException
  import java.io.InputStream
  import java.io.InputStreamReader
  import java.net.URL
  import java.net.URLConnection
  import java.util.Locale
  import java.util.PropertyResourceBundle
  import java.util.ResourceBundle

  override def newBundle(baseName: String,
    locale: Locale,
    format: String,
    loader: ClassLoader,
    reload: Boolean): ResourceBundle = {
    val bundleName = toBundleName(baseName, locale)
    val resourceName = toResourceName(bundleName, "properties")
    var bundle: ResourceBundle = null
    var stream: InputStream = null
    if (reload) {
      val url = loader.getResource(resourceName)
      if (url != null) {
        val connection = url.openConnection()
        if (connection != null) {
          connection.setUseCaches(false)
          stream = connection.getInputStream
        }
      }
    } else {
      stream = loader.getResourceAsStream(resourceName)
    }
    if (stream != null) {
      try {
        bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"))
      } finally {
        stream.close()
      }
    }
    bundle
  }
}