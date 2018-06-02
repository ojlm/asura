package asura.core.cs.operators

import asura.core.cs.FieldTypes
import asura.core.cs.asserts.{AssertResult, FailAssertResult, PassAssertResult}

object Type {

  def apply(src: Any, target: Any): AssertResult = {
    if (null != target && target.isInstanceOf[String]) {
      target.asInstanceOf[String] match {
        case FieldTypes.BOOLEAN =>
          if (src.isInstanceOf[Boolean]) {
            PassAssertResult(1)
          } else {
            FailAssertResult(1)
          }
        case FieldTypes.BYTE =>
          src match {
            case _: Int =>
              val srcInt = src.asInstanceOf[Int]
              if (srcInt >= Byte.MinValue.toInt && srcInt <= Byte.MaxValue.toInt) {
                PassAssertResult(1)
              } else {
                FailAssertResult(1)
              }
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.SHORT =>
          src match {
            case _: Int =>
              val srcInt = src.asInstanceOf[Int]
              if (srcInt >= Short.MinValue.toInt && srcInt <= Short.MaxValue.toInt) {
                PassAssertResult(1)
              } else {
                FailAssertResult(1)
              }
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.INT =>
          src match {
            case _: Int =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.LONG =>
          src match {
            case _: Long =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.FLOAT =>
          src match {
            case _: Float =>
              PassAssertResult(1)
            case _: Double =>
              val dStr = src.asInstanceOf[Double].toString
              if (java.lang.Float.valueOf(dStr).toString.equals(dStr)) {
                PassAssertResult(1)
              } else {
                FailAssertResult(1)
              }
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.DOUBLE =>
          src match {
            case _: Double =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.STRING =>
          src match {
            case _: String =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.ARRAY =>
          src match {
            case _: java.util.Collection[_] =>
              PassAssertResult(1)
            case _: Seq[_] =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case FieldTypes.MAP =>
          src match {
            case _: String =>
              FailAssertResult(1)
            case _: AnyRef =>
              PassAssertResult(1)
            case _ =>
              FailAssertResult(1)
          }
        case _ =>
          FailAssertResult(1, AssertResult.MSG_UNRECOGNIZED_TYPE)
      }
    } else {
      FailAssertResult(1, AssertResult.msgIncomparableSourceType(target))
    }
  }
}
