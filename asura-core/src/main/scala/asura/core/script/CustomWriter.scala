package asura.core.script

import java.io.Writer

class CustomWriter extends Writer {

  var log: (CharSequence) => Unit = null

  override def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
    if (null != log) {
      if (!(len == 1 && cbuf(off).equals('\n'))) {
        log(cbuf.subSequence(off, off + len))
      }
    }
  }

  override def flush(): Unit = {}

  override def close(): Unit = {
    log = null
  }
}
