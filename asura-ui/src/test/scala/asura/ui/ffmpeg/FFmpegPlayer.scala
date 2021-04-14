package asura.ui.ffmpeg

import java.awt.image.BufferedImage
import java.nio.{ByteBuffer, ShortBuffer}
import java.util.concurrent.{Executors, TimeUnit}

import javafx.application.{Application, Platform}
import javafx.scene.Scene
import javafx.scene.image.{Image, ImageView, WritableImage}
import javafx.scene.layout.StackPane
import javafx.stage.{Stage, StageStyle}
import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, SourceDataLine}
import org.bytedeco.javacv.{FFmpegFrameGrabber, Java2DFrameConverter}

object FFmpegPlayer {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[FFmpegPlayer], args: _*)
  }

}

class FFmpegPlayer extends Application {

  var playThread: Thread = null

  override def start(stage: Stage): Unit = {
    val args = getParameters.getRaw
    if (args.size() == 0) {
      Console.err.println("Empty file.")
      System.exit(0);
    } else {
      val filename = getParameters.getRaw.get(0)
      println(s"play file: $filename")
      stage.setTitle(s"playing - $filename")
      stage.setAlwaysOnTop(true)
      stage.initStyle(StageStyle.DECORATED)
      val root = new StackPane()
      val imageView = new ImageView()
      root.getChildren().add(imageView)
      imageView.fitWidthProperty().bind(stage.widthProperty())
      imageView.fitHeightProperty().bind(stage.heightProperty())
      stage.setScene(new Scene(root, 640, 480))
      stage.show()
      playThread = new Thread(() => {
        try {
          val grabber = new FFmpegFrameGrabber(filename)
          grabber.start()
          stage.setWidth(grabber.getImageWidth)
          stage.setHeight(grabber.getImageHeight)
          val audioFormat = new AudioFormat(grabber.getSampleRate, 16, grabber.getAudioChannels, true, true)
          val info = new DataLine.Info(classOf[SourceDataLine], audioFormat)
          val soundLine = AudioSystem.getLine(info).asInstanceOf[SourceDataLine]
          soundLine.open(audioFormat)
          soundLine.start()
          val converter = new Java2DFrameConverter()
          val executor = Executors.newSingleThreadExecutor()
          var over = false
          while (!Thread.interrupted() && !over) {
            val frame = grabber.grab()
            if (frame == null) {
              over = true
            } else {
              if (frame.image != null) {
                val image = converter.convert(frame)
                val fxImage = convertToFxImage(image)
                Platform.runLater(() => {
                  imageView.setImage(fxImage)
                })
              } else if (frame.samples != null) {
                val channelSamplesShortBuffer = frame.samples(0).asInstanceOf[ShortBuffer]
                channelSamplesShortBuffer.rewind()
                val outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity() * 2)
                for (i <- 0 until channelSamplesShortBuffer.capacity()) {
                  val value = channelSamplesShortBuffer.get(i)
                  outBuffer.putShort(value)
                }
                // We need this because soundLine.write ignores interruptions during writing.
                try {
                  executor.submit(() => {
                    soundLine.write(outBuffer.array(), 0, outBuffer.capacity())
                    outBuffer.clear()
                  }).get()
                } catch {
                  case _: InterruptedException => Thread.currentThread().interrupt()
                }
              }
            }
          }
          executor.shutdown()
          executor.awaitTermination(10, TimeUnit.SECONDS)
          soundLine.stop()
          grabber.stop()
          grabber.release()
          Platform.exit()
        } catch {
          case t: Throwable =>
            t.printStackTrace()
            System.exit(1)
        }
      })
    }
    playThread.start()
  }

  override def stop(): Unit = {
    if (playThread != null) playThread.interrupt()
  }

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

}
