package asura.ui.cli.server

import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.time.LocalDateTime

import scala.concurrent.{ExecutionContext, Future}

import asura.ui.cli.codec._
import asura.ui.cli.hub.Hubs.StreamHub
import asura.ui.cli.hub.{Sink, StreamFrame}
import javafx.application.{Application, Platform}
import javafx.event.EventType
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.scene.image._
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Callback
import org.bytedeco.javacv.Java2DFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.opencv.opencv_core.Mat

object ScrcpyWindow {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[ScrcpyWindow], args: _*)
  }
}

class ScrcpyWindow extends Application {

  val device = "bb8695f8"
  val width = 1440
  val height = 3120

  override def start(stage: Stage): Unit = {
    stage.setTitle(device)
    stage.setAlwaysOnTop(true)
    val root = new StackPane()
    val imageView = new ImageView()
    root.getChildren().add(imageView)
    imageView.fitWidthProperty().bind(root.widthProperty())
    imageView.fitHeightProperty().bind(root.heightProperty())
    imageView.setPreserveRatio(true)
    imageView.setFocusTraversable(true)
    imageView.addEventHandler(EventType.ROOT, WindowEventHandler(device))
    val scene = new Scene(root, 360, 720)
    scene.setFill(Color.BLACK)
    stage.setScene(scene)
    stage.show()
    Server(8080, ServerProxyConfig(true, 9221, 5901, true, false)).start()
    val stream = VideoStream.init(null, pixelBufferListener(imageView))
    StreamHub.enter(device, new Sink[StreamFrame] {
      override def write(frame: StreamFrame): Boolean = {
        stream.put(frame)
        true
      }
    })
    Future {
      stream.run()
    }(ExecutionContext.global)
  }

  override def stop(): Unit = {
    Platform.exit()
    System.exit(0)
  }

  def pixelBufferListener(imageView: ImageView): StreamListener = {
    val buffer = ByteBuffer.allocateDirect(width * height * 4)
    val pixelBuffer = new PixelBuffer[ByteBuffer](width, height, buffer, PixelFormat.getByteBgraPreInstance())
    imageView.setImage(new WritableImage(pixelBuffer))
    val callback = new Callback[PixelBuffer[ByteBuffer], Rectangle2D] {
      override def call(param: PixelBuffer[ByteBuffer]): Rectangle2D = null
    }
    new PixelBufferStreamListener(pixelBuffer) {
      override def onUpdate(frame: PixelBuffer[ByteBuffer]): Unit = {
        // println(s"${LocalDateTime.now()}")
        Platform.runLater(() => {
          frame.updateBuffer(callback)
        })
      }
    }
  }

  def matListener(imageView: ImageView): StreamListener = {
    val toMat = new ToMat()
    val toJava2DFrame = new Java2DFrameConverter()

    def convertToFxImage(image: BufferedImage): Image = {
      val wr = new WritableImage(image.getWidth, image.getHeight)
      val pw = wr.getPixelWriter
      for (x <- 0 until image.getWidth) {
        for (y <- 0 until image.getHeight) {
          pw.setArgb(x, y, image.getRGB(x, y))
        }
      }
      new ImageView(wr).getImage
    }

    new ToMatStreamListener(width, height) {
      override def onUpdate(mat: Mat): Unit = {
        println(s"${LocalDateTime.now()}")
        val frame = toMat.convert(mat)
        imageView.setImage(convertToFxImage(toJava2DFrame.convert(frame)))
      }
    }
  }

}
