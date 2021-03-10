package asura.ui.opencv

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.nio.IntBuffer

import asura.ui.model.IntPoint
import javax.swing.WindowConstants
import org.bytedeco.javacpp.indexer.FloatIndexer
import org.bytedeco.javacpp.{DoublePointer, IntPointer}
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv.{CanvasFrame, Java2DFrameConverter}
import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgcodecs._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core.{KeyPointVector, _}

import scala.collection.mutable.ArrayBuffer
import scala.math.round

object OpencvUtils {

  def toBytes(mat: Mat): Array[Byte] = {
    val size = mat.total() * mat.channels()
    val bytes = Array.ofDim[Byte](size.toInt)
    imencode(".jpg", mat, bytes)
    bytes
  }

  def toPoints(vector: KeyPointVector): Seq[IntPoint] = {
    val points = vector.get()
    val res = ArrayBuffer[IntPoint]()
    var last: IntPoint = IntPoint(0, 0)
    points.foreach(p => {
      val x = p.pt().x().toInt
      val y = p.pt().y().toInt
      if (last.x != x && last.y != y) {
        val current = IntPoint(x, y)
        res += current
        last = current
      }
    })
    res.toSeq
  }

  def load(bytes: Array[Byte], flags: Int = IMREAD_COLOR): Mat = {
    imdecode(new Mat(bytes: _*), flags)
  }

  def loadFileAndShowOrExit(file: String, flags: Int = IMREAD_COLOR): Mat = {
    loadAndShowOrExit(new File(file), flags)
  }

  def loadAndShowOrExit(file: File, flags: Int = IMREAD_COLOR): Mat = {
    val image = loadOrExit(file, flags)
    show(image, file.getName)
    image
  }

  def loadOrExit(file: File, flags: Int = IMREAD_COLOR): Mat = {
    val image = imread(file.getAbsolutePath, flags)
    if (image.empty()) {
      println("Couldn't load image: " + file.getAbsolutePath)
      sys.exit(1)
    }
    image
  }

  def show(mat: Mat, title: String): Unit = {
    val converter = new ToMat()
    val canvas = new CanvasFrame(title, 1)
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    canvas.showImage(converter.convert(mat))
  }

  def show(image: Image, title: String): Unit = {
    val canvas = new CanvasFrame(title, 1)
    canvas.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    canvas.showImage(image)
  }

  def drawOnImage(image: Mat, points: Point2fVector): Mat = {
    val dest = image.clone()
    val radius = 5
    val red = new Scalar(0, 0, 255, 0)
    for (i <- 0 until points.size.toInt) {
      val p = points.get(i)
      circle(dest, new Point(round(p.x), round(p.y)), radius, red)
    }
    dest
  }

  def drawOnImage(image: Mat, overlay: Rect, color: Scalar): Mat = {
    val dest = image.clone()
    rectangle(dest, overlay, color)
    dest
  }

  def save(file: File, image: Mat): Unit = {
    imwrite(file.getAbsolutePath, image)
  }

  def toArray(keyPoints: KeyPoint): Array[KeyPoint] = {
    val oldPosition = keyPoints.position()
    val points = for (i <- Array.range(0, keyPoints.capacity.toInt)) yield new KeyPoint(keyPoints.position(i))
    keyPoints.position(oldPosition)
    points
  }

  def toArray(keyPoints: KeyPointVector): Array[KeyPoint] = {
    require(keyPoints.size() <= Int.MaxValue)
    val n = keyPoints.size().toInt
    for (i <- Array.range(0, n)) yield new KeyPoint(keyPoints.get(i))
  }

  def toArray(matches: DMatchVector): Array[DMatch] = {
    require(matches.size() <= Int.MaxValue)
    val n = matches.size().toInt
    for (i <- Array.range(0, n)) yield new DMatch(matches.get(i))
  }

  def toBufferedImage(mat: Mat): BufferedImage = {
    val openCVConverter = new ToMat()
    val java2DConverter = new Java2DFrameConverter()
    java2DConverter.convert(openCVConverter.convert(mat))
  }

  def toPoint(p: Point2f): IntPoint = new IntPoint(round(p.x), round(p.y))

  def toMat8U(src: Mat, doScaling: Boolean = true): Mat = {
    val minVal = new DoublePointer(Double.MaxValue)
    val maxVal = new DoublePointer(Double.MinValue)
    minMaxLoc(src, minVal, maxVal, null, null, new Mat())
    val min = minVal.get(0)
    val max = maxVal.get(0)
    val (scale, offset) = if (doScaling) {
      val s = 255d / (max - min)
      (s, -min * s)
    } else (1d, 0d)
    val dest = new Mat()
    src.convertTo(dest, CV_8U, scale, offset)
    dest
  }

  def toMatPoint2f(points: Seq[Point2f]): Mat = {
    val dest = new Mat(1, points.size, CV_32FC2)
    val indexer = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- points.indices) {
      val p = points(i)
      indexer.put(0, i, 0, p.x)
      indexer.put(0, i, 1, p.y)
    }
    require(dest.checkVector(2) >= 0)
    dest
  }

  def toMatPoint3f(points: Seq[Point3f]): Mat = {
    val dest = new Mat(1, points.size, CV_32FC3)
    val indx = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- points.indices) {
      val p = points(i)
      indx.put(0, i, 0, p.x)
      indx.put(0, i, 1, p.y)
      indx.put(0, i, 2, p.z)
    }
    dest
  }

  def toPoint2fArray(mat: Mat): Array[Point2f] = {
    require(mat.checkVector(2) >= 0, "Expecting a vector Mat")
    val indexer = mat.createIndexer().asInstanceOf[FloatIndexer]
    val size = mat.total.toInt
    val dest = new Array[Point2f](size)
    for (i <- 0 until size) dest(i) = new Point2f(indexer.get(0, i, 0), indexer.get(0, i, 1))
    dest
  }

  def toMat(points: Point2fVector): Mat = {
    val size: Int = points.size.toInt
    val dest = new Mat(1, size, CV_32FC2)
    val indexer = dest.createIndexer().asInstanceOf[FloatIndexer]
    for (i <- 0 until size) {
      val p = points.get(i)
      indexer.put(0, i, 0, p.x)
      indexer.put(0, i, 1, p.y)
    }
    dest
  }

  def toVector(src: Array[DMatch]): DMatchVector = {
    val dest = new DMatchVector(src.length)
    for (i <- src.indices) dest.put(i, src(i))
    dest
  }

  def wrapInMatVector(mat: Mat): MatVector = {
    new MatVector(Array(mat): _*)
  }

  def wrapInIntBuffer(v: Int): IntBuffer = {
    IntBuffer.wrap(Array(v))
  }

  def wrapInIntPointer(v: Int): IntPointer = {
    new IntPointer(1L).put(v)
  }

  def printInfo(mat: Mat, caption: String = ""): Unit = {
    println(
      caption + "\n" +
        s"  cols:     ${mat.cols}\n" +
        s"  rows:     ${mat.rows}\n" +
        s"  depth:    ${mat.depth}\n" +
        s"  channels: ${mat.channels}\n" +
        s"  type:     ${mat.`type`}\n" +
        s"  dims:     ${mat.dims}\n" +
        s"  total:    ${mat.total}\n"
    )
  }

}
