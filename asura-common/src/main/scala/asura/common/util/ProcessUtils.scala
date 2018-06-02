package asura.common.util

import java.io.File

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.sys.process._
import scala.util.Try

object ProcessUtils extends ProcessUtils {
  type ExitValue = Int
  type Stdout = String
  type Stderr = String

  type ExecResult = (ExitValue, Stdout, Stderr)

  trait AsyncExecResult {
    def map[T](f: ExecResult => T): Future[T]

    def foreach(f: ExecResult => Unit): Unit

    def onComplete[T](pf: Try[ExecResult] => T): Unit

    def cancel: Cancelable

    def isRunning: Boolean

    def get: Future[ExecResult]
  }

  type Cancelable = () => Unit

  case class ExecutionCanceled(msg: String) extends Exception(msg)

}

trait ProcessUtils {

  import ProcessUtils._

  def exec(cmd: Seq[String], cwd: File, fn: String => Unit = null): Int = {
    if (null != fn) Process(cmd, cwd).!(ProcessLogger(fn)) else Process(cmd, cwd).!
  }

  def exec(cmd: String): ExecResult = exec(cmd.split(" "))

  def exec(cmd: Seq[String]): ExecResult = {
    val stdout = new OutputBuffer
    val stderr = new OutputBuffer

    Try {
      val process = Process(cmd).run(ProcessLogger(stdout.appendLine, stderr.appendLine))
      process.exitValue()
    }.map((_, stdout.get, stderr.get))
      .recover {
        case t => (-1, "", t.getMessage)
      }.get
  }

  def execAsync(cmd: String)(implicit ec: ExecutionContext): AsyncExecResult = execAsync(cmd.split(" "))

  def execAsync(cmd: Seq[String])(implicit ec: ExecutionContext): AsyncExecResult = {
    new AsyncExecResult {

      val (fut, cancelable) = runAsync(cmd)

      override def cancel: Cancelable = cancelable

      override def onComplete[T](pf: (Try[(ExitValue, Stdout, Stderr)]) => T): Unit = fut.onComplete(pf)

      override def foreach(f: ((ExitValue, Stdout, Stderr)) => Unit): Unit = fut.foreach(f)

      override def isRunning: Boolean = !fut.isCompleted

      override def get: Future[(ExitValue, Stdout, Stderr)] = fut

      override def map[T](f: ((ExitValue, Stdout, Stderr)) => T): Future[T] = fut.map(f)
    }
  }

  private def runAsync(cmd: Seq[String])(implicit ec: ExecutionContext): (Future[ExecResult], Cancelable) = {
    val p = Promise[ExecResult]

    val stdout = new OutputBuffer
    val stderr = new OutputBuffer

    val process = Process(cmd).run(ProcessLogger(stdout.appendLine, stderr.appendLine))
    p.tryCompleteWith(Future(process.exitValue).map(c => (c, stdout.get, stderr.get)))

    val cancelFunc = () => {
      p.tryFailure(new ExecutionCanceled(s"Process: '${cmd.mkString(" ")}' canceled"))
      process.destroy()
    }
    (p.future, cancelFunc)
  }

  class OutputBuffer {
    private val sb = new StringBuilder

    def append(s: String): Unit = sb.append(s)

    def appendLine(s: String): Unit = append(s + "\n")

    def get: String = sb.toString
  }

}
