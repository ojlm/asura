package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm
import asura.ui.cli.store.lucene.query.SearchTerm.ExactLongSearchTerm
import org.apache.lucene.document.{Document, LongPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

object LongField extends FieldSupport[Long] {

  override def store(field: Field[Long], value: Long, document: Document): Unit = {
    document.add(new StoredField(field.name, value))
  }

  override def filter(field: Field[Long], value: Long, document: Document): Unit = {
    document.add(new LongPoint(field.filterName, value))
  }

  override def sorted(field: Field[Long], value: Long, document: Document): Unit = {
    document.add(new NumericDocValuesField(field.sortName, value))
  }

  override def sortFieldType: SortField.Type = SortField.Type.LONG

  override def fromLucene(fields: List[IndexableField]): Long = {
    fields.head.numericValue().longValue()
  }

  override def searchTerm(fv: FieldValue[Long]): SearchTerm = {
    ExactLongSearchTerm(fv.field, fv.value)
  }

}
