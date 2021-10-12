package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm
import asura.ui.cli.store.lucene.query.SearchTerm.ExactBytesSearchTerm
import org.apache.lucene.document.{BinaryDocValuesField, BinaryPoint, Document, StoredField}
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField
import org.apache.lucene.util.BytesRef

object BytesField extends FieldSupport[BytesRef] {

  override def store(field: Field[BytesRef], value: BytesRef, document: Document): Unit = {
    document.add(new StoredField(field.name, value))
  }

  override def filter(field: Field[BytesRef], value: BytesRef, document: Document): Unit = {
    document.add(new BinaryPoint(field.filterName, value.bytes))
  }

  override def sorted(field: Field[BytesRef], value: BytesRef, document: Document): Unit = {
    document.add(new BinaryDocValuesField(field.name, value))
  }

  override def sortFieldType: SortField.Type = SortField.Type.DOC

  override def fromLucene(fields: List[IndexableField]): BytesRef = {
    fields.head.binaryValue()
  }

  override def searchTerm(fv: FieldValue[BytesRef]): SearchTerm = {
    ExactBytesSearchTerm(fv.field, fv.value.bytes)
  }

}
