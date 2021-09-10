package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm
import asura.ui.cli.store.lucene.query.SearchTerm.ExactDoubleSearchTerm
import org.apache.lucene.document.{Document, DoubleDocValuesField, DoublePoint, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

object DoubleField extends FieldSupport[Double] {

  override def store(field: Field[Double], value: Double, document: Document): Unit = {
    document.add(new StoredField(field.name, value))
  }

  override def filter(field: Field[Double], value: Double, document: Document): Unit = {
    document.add(new DoublePoint(field.filterName, value))
  }

  override def sorted(field: Field[Double], value: Double, document: Document): Unit = {
    document.add(new DoubleDocValuesField(field.sortName, value))
  }

  override def sortFieldType: SortField.Type = SortField.Type.DOUBLE

  override def fromLucene(fields: List[IndexableField]): Double = {
    fields.head.numericValue().doubleValue()
  }

  override def searchTerm(fv: FieldValue[Double]): SearchTerm = {
    ExactDoubleSearchTerm(fv.field, fv.value)
  }

}
