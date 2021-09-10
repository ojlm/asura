package asura.ui.cli.store.lucene.query

import asura.ui.cli.store.lucene.field.{Field, SpatialPoint}
import org.apache.lucene.document.LatLonDocValuesField
import org.apache.lucene.search.SortField

trait Sort {
  def sortField(): SortField
}

object Sort {

  def apply[T](field: Field[T], reverse: Boolean = false): Sort = {
    assert(field.sortable, s"$field is not sortable")
    FieldSort(field, reverse)
  }

  case object Score extends Sort {
    override def sortField(): SortField = SortField.FIELD_SCORE
  }

  case object IndexOrder extends Sort {
    override def sortField(): SortField = SortField.FIELD_DOC
  }

  case class FieldSort[T](field: Field[T], reverse: Boolean) extends Sort {
    override def sortField(): SortField = {
      new SortField(field.sortName, field.support.sortFieldType, reverse)
    }
  }

  case class NearestSort(field: Field[SpatialPoint], point: SpatialPoint) extends Sort {
    override def sortField(): SortField = {
      LatLonDocValuesField.newDistanceSort(field.sortName, point.latitude, point.longitude)
    }
  }

}
