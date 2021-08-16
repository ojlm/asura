package asura.ui.cli.server

import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.time.LocalDateTime

import asura.ui.codec._
import asura.ui.hub.Hubs.RenderingFrameHub
import asura.ui.cli.server.ServerProxyConfig.FixedPortSelector
import asura.ui.cli.window.BgraPixelBufferStreamListener
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
  val windowWidth = 280
  val frameWidth = 1440
  val frameHeight = 3120

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
    imageView.addEventHandler(EventType.ROOT, WindowEventHandler(device, Size(frameWidth, frameHeight)))
    val scene = new Scene(root, windowWidth, frameHeight * windowWidth / frameWidth)
    scene.setFill(Color.BLACK)
    stage.setScene(scene)
    stage.show()
    Server(8080, ServerProxyConfig(true, FixedPortSelector(9221), 5901, true, false)).start()
    RenderingFrameHub.enter(device, pixelBufferListener(imageView))
  }

  override def stop(): Unit = {
    Platform.exit()
    System.exit(0)
  }

  def pixelBufferListener(imageView: ImageView): BgraPixelBufferStreamListener = {
    val buffer = ByteBuffer.allocateDirect(frameWidth * frameHeight * 4)
    val pixelBuffer = new PixelBuffer[ByteBuffer](frameWidth, frameHeight, buffer, PixelFormat.getByteBgraPreInstance())
    imageView.setImage(new WritableImage(pixelBuffer))
    val callback = new Callback[PixelBuffer[ByteBuffer], Rectangle2D] {
      override def call(param: PixelBuffer[ByteBuffer]): Rectangle2D = null
    }
    new BgraPixelBufferStreamListener(pixelBuffer) {
      override def onUpdate(frame: PixelBuffer[ByteBuffer]): Boolean = {
        // println(s"${LocalDateTime.now()}")
        Platform.runLater(() => {
          frame.updateBuffer(callback)
        })
        true
      }
    }
  }

  def matListener(imageView: ImageView): ToMatStreamListener = {
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

    new ToMatStreamListener(frameWidth, frameHeight) {
      override def onUpdate(mat: Mat): Boolean = {
        println(s"${LocalDateTime.now()}")
        val frame = toMat.convert(mat)
        imageView.setImage(convertToFxImage(toJava2DFrame.convert(frame)))
        true
      }
    }
  }

}
