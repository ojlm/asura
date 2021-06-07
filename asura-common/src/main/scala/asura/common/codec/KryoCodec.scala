package asura.common.codec

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}

object KryoCodec {

  private val localInstance: ThreadLocal[Kryo] = ThreadLocal.withInitial(() => {
    val kryo: Kryo = new Kryo()
    kryo.setRegistrationRequired(false)
    kryo
  })

  def toBytes(obj: Object): Array[Byte] = {
    val output = new Output(1024, -1)
    localInstance.get.writeObject(output, obj)
    output.toBytes
  }

  def fromBytes[T](bytes: Array[Byte], clazz: Class[T]): T = {
    val input = new Input(bytes, 0, bytes.length)
    localInstance.get.readObject(input, clazz)
  }

  def get: Kryo = localInstance.get

}
