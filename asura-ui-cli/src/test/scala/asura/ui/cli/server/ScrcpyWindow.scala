package asura.ui.cli.server

import java.time.LocalDateTime

import javax.swing.WindowConstants

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.codec.{ToMatStreamListener, VideoStream}
import asura.ui.cli.hub.{FrameSink, StreamFrame, StreamHub}
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.bytedeco.opencv.opencv_core.Mat

object ScrcpyWindow {

  def main(args: Array[String]): Unit = {
    val device = "HD1910"
    val canvas = new CanvasFrame(device, 1)
    canvas.setCanvasSize(360, 640)
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    // start server
    Server(8080, ServerProxyConfig(true, 9221, 5901, true, false)).start()
    val converter = new ToIplImage()
    val stream = VideoStream.init(null, new ToMatStreamListener(1440, 3120) {
      override def onOpenCvFrame(frame: Mat): Unit = {
        println(s"${LocalDateTime.now()}")
        canvas.showImage(converter.convert(frame))
      }
    })
    StreamHub.enter(device, new FrameSink {
      override def write(frame: StreamFrame): Boolean = {
        stream.put(frame)
        true
      }

      override def close(): Unit = {}
    })
    Future {
      stream.run()
    }(ExecutionContext.global)
  }

}
