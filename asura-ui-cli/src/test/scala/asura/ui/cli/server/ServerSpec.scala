package asura.ui.cli.server

object ServerSpec {

  def main(args: Array[String]): Unit = {
    val s = Server(8080, ServerProxyConfig(true, 9221, 5901))
    s.start()
    println("server started.")
  }

}
