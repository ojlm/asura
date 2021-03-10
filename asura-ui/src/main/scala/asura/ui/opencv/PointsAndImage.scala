package asura.ui.opencv

import asura.ui.model.IntPoint

case class PointsAndImage(
                           points: Seq[IntPoint],
                           image: Array[Byte],
                         )
