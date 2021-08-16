package asura.ui.codec

case class FpsCounter(
                       rendered: Int,
                       skipped: Int,
                       nextTimestamp: Long,
                     )

object FpsCounter {

  def init(): FpsCounter = {
    FpsCounter(0, 0, 0L)
  }

}
