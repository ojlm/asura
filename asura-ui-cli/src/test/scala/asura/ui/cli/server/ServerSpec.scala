package asura.ui.cli.server

object ServerSpec {

  def main(args: Array[String]): Unit = {
    val s = Server(8080, ServerProxyConfig(true, 9222, 5901))
    s.start()
    println("server started.")
  }

}
