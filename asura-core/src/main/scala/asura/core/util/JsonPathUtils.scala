package asura.core.util

import java.util

import com.jayway.jsonpath
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.{Configuration, JsonPath}

object JsonPathUtils {

  Configuration.setDefaults(new Configuration.Defaults {
    val jsonProvider = new JacksonJsonProvider()
    val mappingProvider = new JacksonMappingProvider()

    override def options(): util.Set[jsonpath.Option] = {
      return util.EnumSet.noneOf(classOf[jsonpath.Option])
    }
  })

  /** use java type system */
  def parse(json: String): AnyRef = {
    Configuration.defaultConfiguration().jsonProvider().parse(json)
  }

  /** use java type system */
  def read[T](json: String, path: String): T = {
    JsonPath.read[T](json, path)
  }

  /** use java type system */
  def read[T](doc: Object, path: String): T = {
    JsonPath.read[T](doc, path)
  }
}
