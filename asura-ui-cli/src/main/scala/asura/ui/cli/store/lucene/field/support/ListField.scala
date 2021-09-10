package asura.ui.cli.store.lucene.field.support

import asura.ui.cli.store.lucene.field.{Field, FieldSupport, FieldValue}
import asura.ui.cli.store.lucene.query.SearchTerm.GroupedSearchTerm
import asura.ui.cli.store.lucene.query.{Condition, SearchTerm}
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

class ListField[T](underlying: FieldSupport[T]) extends FieldSupport[List[T]] {

  override def store(field: Field[List[T]], value: List[T], document: Document): Unit = {
    value.foreach(item => {
      underlying.store(field.asInstanceOf[Field[T]], item, document)
    })
  }

  override def filter(field: Field[List[T]], value: List[T], document: Document): Unit = {
    value.foreach(item => {
      underlying.filter(field.asInstanceOf[Field[T]], item, document)
    })
  }

  override def sorted(field: Field[List[T]], value: List[T], document: Document): Unit = {
    value.foreach(item => {
      underlying.store(field.asInstanceOf[Field[T]], item, document)
    })
  }

  override def sortFieldType: SortField.Type = underlying.sortFieldType

  override def fromLucene(fields: List[IndexableField]): List[T] = {
    fields.grouped(1).toList.map(underlying.fromLucene)
  }

  override def searchTerm(fv: FieldValue[List[T]]): SearchTerm = {
    GroupedSearchTerm(0, fv.value.map(v => (underlying.searchTerm(FieldValue(fv.field.asInstanceOf[Field[T]], v)), Condition.MUST)))
  }

}
