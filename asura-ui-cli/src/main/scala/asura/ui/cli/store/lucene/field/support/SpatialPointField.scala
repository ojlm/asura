package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue, SpatialPoint}
import asura.ui.cli.store.lucene.query.SearchTerm
import asura.ui.cli.store.lucene.query.SearchTerm.SpatialDistanceTerm
import org.apache.lucene.document.{Document, LatLonDocValuesField, LatLonPoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

object SpatialPointField extends FieldSupport[SpatialPoint] {

  override def store(field: Field[SpatialPoint], value: SpatialPoint, document: Document): Unit = {
    document.add(new StoredField(field.name, value.toString))
  }

  override def filter(field: Field[SpatialPoint], value: SpatialPoint, document: Document): Unit = {
    document.add(new LatLonPoint(field.filterName, value.latitude, value.longitude))
  }

  override def sorted(field: Field[SpatialPoint], value: SpatialPoint, document: Document): Unit = {
    document.add(new LatLonDocValuesField(field.sortName, value.latitude, value.longitude))
  }

  override def sortFieldType: SortField.Type = SortField.Type.SCORE

  override def fromLucene(fields: List[IndexableField]): SpatialPoint = {
    SpatialPoint(fields.head.stringValue())
  }

  override def searchTerm(fv: FieldValue[SpatialPoint]): SearchTerm = {
    SpatialDistanceTerm(fv.field, fv.value, 1)
  }

}
