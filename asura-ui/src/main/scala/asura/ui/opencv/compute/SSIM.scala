package asura.ui.opencv.compute

import org.bytedeco.opencv.global.opencv_core._
import org.bytedeco.opencv.global.opencv_imgproc._
import org.bytedeco.opencv.opencv_core._

/**
 * http://www.cns.nyu.edu/~lcv/ssim/
 * https://docs.opencv.org/4.5.2/d5/dc4/tutorial_video_input_psnr_ssim.html
 */
class SSIM(c1: Double = 6.5025, c2: Double = 58.5225, d: Int = CV_32F) extends Compute {

  override def compute(image: Mat): Mat = {
    throw new RuntimeException("Not supported")
  }

  override def compare(reference: Mat, second: Mat): ScoreResult = {
    // INITS
    val i1 = new Mat()
    val i2 = new Mat()
    reference.convertTo(i1, d)
    second.convertTo(i2, d)
    val i2_2 = i2.mul(i2)
    val i1_2 = i1.mul(i1)
    val i1_i2 = i1.mul(i2)
    // PRELIMINARY COMPUTING
    val mu1 = new Mat()
    val mu2 = new Mat()
    GaussianBlur(i1, mu1, new Size(11, 11), 1.5)
    GaussianBlur(i2, mu2, new Size(11, 11), 1.5)
    val mu1_2 = mu1.mul(mu1)
    val mu2_2 = mu2.mul(mu2)
    val mu1_mu2 = mu1.mul(mu2)
    var sigma1_2 = new Mat()
    var sigma2_2 = new Mat()
    var sigma12 = new Mat()
    GaussianBlur(i1_2.asMat(), sigma1_2, new Size(11, 11), 1.5)
    sigma1_2 = subtract(sigma1_2, mu1_2).asMat()
    GaussianBlur(i2_2.asMat(), sigma2_2, new Size(11, 11), 1.5)
    sigma2_2 = subtract(sigma2_2, mu2_2).asMat()
    GaussianBlur(i1_i2.asMat(), sigma12, new Size(11, 11), 1.5)
    sigma12 = subtract(sigma12, mu1_mu2).asMat()
    // FORMULA
    var t1 = add(multiply(2, mu1_mu2), new Scalar(c1))
    var t2 = add(multiply(2, sigma12), new Scalar(c2))
    val t3 = t1.mul(t2) // t3 = ((2*mu1_mu2 + C1).*(2*sigma12 + C2))
    t1 = add(add(mu1_2, mu2_2), new Scalar(c1))
    t2 = add(add(sigma1_2, sigma2_2), new Scalar(c2))
    t1 = t1.mul(t2) // t1 =((mu1_2 + mu2_2 + C1).*(sigma1_2 + sigma2_2 + C2))
    val ssimMap = new Mat()
    divide(t3.asMat(), t1.asMat(), ssimMap) // ssim_map =  t3./t1
    val scalar = mean(ssimMap) // scalar = average of ssim map, (R, G & B SSIM index)
    if (ssimMap.channels() == 1) {
      ScoreResult(scalar.get(0), ssimMap)
    } else {
      ScoreResult((scalar.get(0) + scalar.get(1) + scalar.get(2)) / 3, ssimMap)
    }
  }

}
