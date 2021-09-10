package asura.ui.cli.store.lucene.field

import asura.ui.cli.store.lucene.query.SearchTerm
import org.apache.lucene.document.Document
import org.apache.lucene.index.IndexableField
import org.apache.lucene.search.SortField

trait FieldSupport[T] {

  def store(field: Field[T], value: T, document: Document): Unit

  def filter(field: Field[T], value: T, document: Document): Unit

  def sorted(field: Field[T], value: T, document: Document): Unit

  def sortFieldType: SortField.Type

  def separateFilter: Boolean = true

  def fromLucene(fields: List[IndexableField]): T

  def searchTerm(fv: FieldValue[T]): SearchTerm

}
