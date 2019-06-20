package asura.core.script.builtin

object Functions {

  val console =
    """
      |var console = {
      |  log: print
      |};
    """.stripMargin

  def exports = Seq(console).mkString
}
