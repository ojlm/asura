package asura.core.script.builtin

object Functions {

  val console =
    """
      |var console = {
      |  log: print
      |};
    """.stripMargin

  val atob =
    """
      |var String = Java.type('java.lang.String');
      |var Base64 = Java.type('java.util.Base64');
      |function atob(src) { return new String(Base64.getDecoder().decode(src));}
    """.stripMargin

  val btoa =
    """
      |var Base64 = Java.type('java.util.Base64');
      |function btoa(src) { return Base64.getEncoder().encodeToString(src.getBytes());}
    """.stripMargin

  def exports = Seq(console, atob, btoa).mkString
}
