package asura.ui.cli.window

import java.nio.ByteBuffer

import asura.ui.cli.codec.{Size, WindowEventHandler}
import asura.ui.cli.hub.Hubs.RenderingFrameHub
import asura.ui.cli.hub.Sink
import asura.ui.cli.window.DeviceWindow.EMPTY_CALLBACK
import javafx.application.Platform
import javafx.event.EventType
import javafx.geometry.Rectangle2D
import javafx.scene.Scene
import javafx.scene.image.{ImageView, PixelBuffer, WritableImage}
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.util.Callback
import org.bytedeco.ffmpeg.avutil.AVFrame

class DeviceWindow(stage: Stage, width: Int) extends Sink[AVFrame] {

  RenderingFrameHub.enter(stage.getTitle, this)
  private var bufferHolder: BgraPixelBufferHolder = null

  def initBufferAndShow(frameSize: Size): Unit = {
    bufferHolder = BgraPixelBufferHolder(frameSize)
    val root = new StackPane()
    val imageView = new ImageView()
    root.getChildren().add(imageView)
    imageView.fitWidthProperty().bind(root.widthProperty())
    imageView.fitHeightProperty().bind(root.heightProperty())
    imageView.setPreserveRatio(true)
    imageView.setFocusTraversable(true)
    imageView.setImage(new WritableImage(bufferHolder.buffer))
    imageView.addEventHandler(EventType.ROOT, WindowEventHandler(stage.getTitle, frameSize))
    val scene = new Scene(root, width, frameSize.height * width / frameSize.width)
    scene.setFill(Color.BLACK)
    UiThread.run {
      stage.setScene(scene)
      stage.show()
    }
  }

  override def active(params: Object): Unit = {
    if (params.isInstanceOf[Size]) {
      initBufferAndShow(params.asInstanceOf[Size])
    }
  }

  override def write(frame: AVFrame): Boolean = {
    if (bufferHolder == null) {
      initBufferAndShow(Size(frame.width(), frame.height()))
    }
    bufferHolder.write(frame)
    Platform.runLater(() => {
      bufferHolder.buffer.updateBuffer(EMPTY_CALLBACK)
    })
    true
  }

  override def close(): Unit = {
    if (bufferHolder != null) bufferHolder.free()
    UiThread.run {
      stage.close()
    }
  }

}

object DeviceWindow {

  val EMPTY_CALLBACK = new Callback[PixelBuffer[ByteBuffer], Rectangle2D] {
    override def call(param: PixelBuffer[ByteBuffer]): Rectangle2D = null
  }

  def apply(
             device: String,
             width: Int = 280,
             alwaysOnTop: Boolean = false,
             oldStage: Stage = null,
           ): DeviceWindow = {
    val stage = if (oldStage != null) oldStage else new Stage()
    stage.setTitle(device)
    stage.setAlwaysOnTop(alwaysOnTop)
    new DeviceWindow(stage, width)
  }

}
