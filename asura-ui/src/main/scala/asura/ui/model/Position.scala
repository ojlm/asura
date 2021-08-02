package asura.ui.model

import java.util.{Objects, Random}

import asura.ui.util.TypeConverters

case class Position(
                     var x: Int,
                     var y: Int,
                     var width: Int,
                     var height: Int
                   ) extends Region {

  var value: Any = null

  def center(): IntPoint = {
    IntPoint(x + width / 2, y + height / 2)
  }

  def inArea(px: Int, py: Int): Boolean = {
    px >= x && px <= x + width && py >= y && py <= y + height
  }

  def randomPoint(random: Random): (Int, Int) = {
    (x + random.nextInt(width), y + random.nextInt(height))
  }

}

object Position {

  def apply(exp: Object, parent: Position): Position = {
    Objects.requireNonNull(parent, "parent is null")
    val func = (tuple: (Int, Double)) => {
      val (num, percent) = tuple
      if (num != -1) {
        if (num > parent.width || num > parent.height) {
          throw new RuntimeException("num exceeds the parent's region")
        } else {
          Position(
            parent.x + (parent.width - num) / 2, parent.y + (parent.height - num) / 2,
            num, num
          )
        }
      } else {
        val width = (parent.width * percent).toInt
        val height = (parent.height * percent).toInt
        Position(
          parent.x + (parent.width - width) / 2, parent.y + (parent.height - height) / 2,
          width, height
        )
      }
    }
    if (exp.isInstanceOf[String]) {
      val parts = exp.asInstanceOf[String].split(",")
      parts.length match {
        case 1 => func(getParseResult(parts(0)))
        case 2 => apply(parts(0).trim, parts(1).trim, parent)
        case 4 => apply(parts(0).trim, parts(1).trim, parts(2).trim, parts(3).trim, parent)
        case _ => throw new IllegalArgumentException(s"Illegal expression: $exp")
      }
    } else {
      func(getParseResult(exp))
    }
  }

  def apply(x: Object, y: Object, parent: Position): Position = {
    Objects.requireNonNull(parent, "parent is null")
    val tupleX = getParseResult(x)
    var posX, posWidth, posY, posHeight = 0
    posWidth = if (tupleX._1 != -1) {
      if (tupleX._1 > parent.width) {
        throw new RuntimeException("num exceeds the parent's region")
      } else {
        tupleX._1
      }
    } else {
      (parent.width * tupleX._2).toInt
    }
    posX = parent.x + (parent.width - posWidth) / 2
    val tupleY = getParseResult(y)
    posHeight = if (tupleY._1 != -1) {
      if (tupleY._1 > parent.height) {
        throw new RuntimeException("num exceeds the parent's region")
      } else {
        tupleY._1
      }
    } else {
      (parent.height * tupleY._2).toInt
    }
    posY = parent.y + (parent.height - posHeight) / 2
    Position(posX, posY, posWidth, posHeight)
  }

  def apply(x: Object, y: Object, width: Object, height: Object, parent: Position): Position = {
    Objects.requireNonNull(parent, "parent is null")
    val tupleX = getParseResult(x)
    val posX = if (tupleX._1 != -1) {
      parent.x + tupleX._1
    } else {
      parent.x + (parent.width * tupleX._2).toInt
    }
    if (posX > (parent.x + parent.width)) {
      throw new RuntimeException(s"x:$posX exceeds the parent's region")
    }
    val tupleWidth = getParseResult(width)
    val posWidth = if (tupleWidth._1 != -1) {
      if (tupleWidth._1 > parent.width) throw new RuntimeException(s"width:${tupleWidth._1} exceeds the parent's region")
      tupleWidth._1
    } else {
      (parent.width * tupleWidth._2).toInt
    }
    val tupleY = getParseResult(y)
    val posY = if (tupleY._1 != -1) {
      parent.y + tupleY._1
    } else {
      parent.y + (parent.height * tupleY._2).toInt
    }
    if (posY > (parent.y + parent.height)) {
      throw new RuntimeException(s"y:$posY exceeds the parent's region")
    }
    val tupleHeight = getParseResult(height)
    val posHeight = if (tupleHeight._1 != -1) {
      if (tupleHeight._1 > parent.height) throw new RuntimeException(s"height:${tupleHeight._1} exceeds the parent's region")
      tupleHeight._1
    } else {
      (parent.width * tupleHeight._2).toInt
    }
    Position(posX, posY, posWidth, posHeight)
  }

  def apply(x: Int, y: Int, width: Int, height: Int, value: Any): Position = {
    val position = Position(x, y, width, height)
    position.value = value
    position
  }

  def apply(position: java.util.Map[String, AnyRef]): Position = {
    Position(
      TypeConverters.toInt(position.get("x")),
      TypeConverters.toInt(position.get("y")),
      TypeConverters.toInt(position.get("width")),
      TypeConverters.toInt(position.get("height")),
    )
  }

  def dealDoubleNum(num: Double): (Int, Double) = {
    if (num == 0 || num >= 1) {
      (num.toInt, -1)
    } else if (num > 0) {
      (-1, num)
    } else {
      throw new RuntimeException("number must not be negative")
    }
  }

  def getParseResult(exp: Object): (Int, Double) = {
    if (exp.isInstanceOf[String]) {
      val value = exp.asInstanceOf[String]
      try {
        dealDoubleNum(java.lang.Double.valueOf(value))
      } catch {
        case _: Throwable => throw new RuntimeException(s"expression must be a number: $value")
      }
    } else if (exp.isInstanceOf[Integer]) {
      val num = exp.asInstanceOf[Integer]
      if (num < 0) {
        throw new RuntimeException("number must not be negative")
      } else {
        (num, -1)
      }
    } else if (exp.isInstanceOf[Double]) {
      dealDoubleNum(exp.asInstanceOf[Double])
    } else if (exp.isInstanceOf[Float]) {
      dealDoubleNum(exp.asInstanceOf[Float].toDouble)
    } else {
      throw new RuntimeException("Illegal expression")
    }
  }

}
