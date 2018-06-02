package asura.common.config

import java.util.Properties

import com.typesafe.config.Config

object PropertiesConversions {

  implicit def toProperties(config: Config): Properties = {
    val properties = new Properties()
    config.entrySet().forEach(entry => {
      properties.put(entry.getKey, config.getString(entry.getKey))
    })
    properties
  }
}
