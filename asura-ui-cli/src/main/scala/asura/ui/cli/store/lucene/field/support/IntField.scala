package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm
import asura.ui.cli.store.lucene.query.SearchTerm.ExactIntSearchTerm
import org.apache.lucene.document.{Document, IntPoint, NumericDocValuesField, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

object IntField extends FieldSupport[Int] {

  override def store(field: Field[Int], value: Int, document: Document): Unit = {
    document.add(new StoredField(field.name, value))
  }

  override def filter(field: Field[Int], value: Int, document: Document): Unit = {
    document.add(new IntPoint(field.filterName, value))
  }

  override def sorted(field: Field[Int], value: Int, document: Document): Unit = {
    document.add(new NumericDocValuesField(field.sortName, value))
  }

  override def sortFieldType: SortField.Type = SortField.Type.INT

  override def fromLucene(fields: List[IndexableField]): Int = {
    fields.head.numericValue().intValue()
  }

  override def searchTerm(fv: FieldValue[Int]): SearchTerm = {
    ExactIntSearchTerm(fv.field, fv.value)
  }

}
