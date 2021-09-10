package asura.ui.cli.store.lucene

import asura.ui.cli.store.lucene.field.support._
import asura.ui.cli.store.lucene.field.{FieldSupport, SpatialPoint}

trait ImplicitField {

  implicit def stringSupport: FieldSupport[String] = StringField

  implicit def booleanSupport: FieldSupport[Boolean] = BooleanField

  implicit def intSupport: FieldSupport[Int] = IntField

  implicit def longSupport: FieldSupport[Long] = LongField

  implicit def doubleSupport: FieldSupport[Double] = DoubleField

  implicit def spatialPointSupport: FieldSupport[SpatialPoint] = SpatialPointField

  implicit val listStringSupport: FieldSupport[List[String]] = new ListField[String](stringSupport)
  implicit val listBooleanSupport: FieldSupport[List[Boolean]] = new ListField[Boolean](booleanSupport)
  implicit val listIntSupport: FieldSupport[List[Int]] = new ListField[Int](intSupport)
  implicit val listLongSupport: FieldSupport[List[Long]] = new ListField[Long](longSupport)
  implicit val listDoubleSupport: FieldSupport[List[Double]] = new ListField[Double](doubleSupport)
  implicit val listSpatialSupport: FieldSupport[List[SpatialPoint]] = new ListField[SpatialPoint](spatialPointSupport)

}
