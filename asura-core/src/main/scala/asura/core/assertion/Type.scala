package asura.core.assertion

import asura.core.assertion.engine.{AssertResult, FailAssertResult, PassAssertResult}

import scala.concurrent.Future

case class Type() extends Assertion {

  override val name: String = Assertions.TYPE

  override def assert(actual: Any, expect: Any): Future[AssertResult] = {
    Future.successful(Type.apply(actual, expect))
  }

}

object Type {

  def apply(actual: Any, expect: Any): AssertResult = {
    if (null != expect && expect.isInstanceOf[String]) {
      expect.asInstanceOf[String] match {
        case FieldTypes.BOOLEAN =>
          if (actual.isInstanceOf[Boolean]) {
            PassAssertResult(1)
          } else {
            FailAssertResult(1)
          }
        case FieldTypes.BYTE =>
          actual match {
            case _: Int =>
              val srcInt = actual.asInstanceOf[Int]
              if (srcInt >= Byte.MinValue.toInt && srcInt <= Byte.MaxValue.toInt) {
                PassAssertResult(1)
              } else {
                FailAssertResult(1)
              }
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.SHORT =>
          actual match {
            case _: Int =>
              val srcInt = actual.asInstanceOf[Int]
              if (srcInt >= Short.MinValue.toInt && srcInt <= Short.MaxValue.toInt) {
                PassAssertResult(1)
              } else {
                FailAssertResult(1)
              }
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.INT =>
          actual match {
            case _: Int =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.LONG =>
          actual match {
            case _: Long =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.FLOAT =>
          actual match {
            case _: Float =>
              PassAssertResult(1)
            case _: Double =>
              val dStr = actual.asInstanceOf[Double].toString
              if (java.lang.Float.valueOf(dStr).toString.equals(dStr)) {
                PassAssertResult(1)
              } else {
                FailAssertResult(1)
              }
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.DOUBLE =>
          actual match {
            case _: Double =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.STRING =>
          actual match {
            case _: String =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.ARRAY =>
          actual match {
            case _: java.util.Collection[_] =>
              PassAssertResult(1)
            case _: Seq[_] =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.MAP =>
          actual match {
            case _: String =>
              FailAssertResult(1)
            case _: AnyRef =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.NULL =>
          if (null == actual) PassAssertResult(1) else FailAssertResult(1)
        case _ =>
          FailAssertResult(1, AssertResult.MSG_UNRECOGNIZED_TYPE)
      }
    } else {
      FailAssertResult(1, AssertResult.msgIncomparableSourceType(expect))
    }
  }

}
