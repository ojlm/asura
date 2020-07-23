package asura.core.script.function

import scala.collection.mutable

object Functions {

  private val transforms = mutable.HashMap[String, TransformFunction]()

  val buildIn = Seq(ToInteger(), ToLong(), ToMap(), ToString(), Atob(), Btoa(), Script())

  buildIn.foreach(register(_))

  def register(func: TransformFunction): Unit = {
    transforms += (func.name -> func)
  }

  def getTransform(name: String): Option[TransformFunction] = transforms.get(name)

  def getAllTransforms() = transforms.values
}
