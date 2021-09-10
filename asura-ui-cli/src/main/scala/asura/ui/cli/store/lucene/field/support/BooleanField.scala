package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm
import asura.ui.cli.store.lucene.query.SearchTerm.ExactBooleanSearchTerm
import org.apache.lucene.document.{Document, IntPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

object BooleanField extends FieldSupport[Boolean] {

  override def store(field: Field[Boolean], value: Boolean, document: Document): Unit = {
    document.add(new StoredField(field.name, if (value) 1 else 0))
  }

  override def filter(field: Field[Boolean], value: Boolean, document: Document): Unit = {
    document.add(new IntPoint(field.filterName, if (value) 1 else 0))
  }

  override def sorted(field: Field[Boolean], value: Boolean, document: Document): Unit = {
    document.add(new NumericDocValuesField(field.sortName, if (value) 1 else 0))
  }

  override def sortFieldType: SortField.Type = SortField.Type.INT

  override def fromLucene(fields: List[IndexableField]): Boolean = {
    if (fields.head.numericValue().intValue() == 1) true else false
  }

  override def searchTerm(fv: FieldValue[Boolean]): SearchTerm = {
    ExactBooleanSearchTerm(fv.field, fv.value)
  }

}
