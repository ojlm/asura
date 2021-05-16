package asura.ui.cli.codec

case class FpsCounter(
                       nextTimestamp: Long,
                     )

object FpsCounter {

  def init(): FpsCounter = {
    FpsCounter(0L)
  }

}
