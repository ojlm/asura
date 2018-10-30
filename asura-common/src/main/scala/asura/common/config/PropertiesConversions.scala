package asura.common.config

import java.util.Properties

import com.typesafe.config.Config

object PropertiesConversions {

  def toProperties(configs: Config*): Properties = {
    val properties = new Properties()
    configs.foreach(config => {
      config.entrySet().forEach(entry => {
        properties.put(entry.getKey, config.getString(entry.getKey))
      })
    })
    properties
  }
}
